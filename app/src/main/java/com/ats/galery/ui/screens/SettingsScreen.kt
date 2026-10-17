package com.ats.galery.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ats.galery.R
import com.ats.galery.data.MediaSort
import com.ats.galery.data.ThumbRatio
import com.ats.galery.media.MediaUiState
import com.ats.galery.media.MediaViewModel
import com.ats.galery.settings.GridEntrance
import com.ats.galery.settings.ThemeMode
import kotlin.math.roundToInt

/** Tab "Setelan". */
@Composable
fun SettingsScreen(
    ui: MediaUiState,
    vm: MediaViewModel,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val s = ui.settings
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Inset di luar area gulir: konten pertama tidak lagi tertutup bar atas
            // maupun bar navigasi bawah.
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        SettingsSection(title = "Tema") {
            LabeledRow(title = "Mode tema")
            ChipRow(
                options = listOf("Ikuti sistem", "Terang", "Gelap"),
                selectedIndex = s.themeMode.ordinal,
                onSelect = { vm.setThemeMode(ThemeMode.entries[it]) },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    title = "Warna dinamis",
                    subtitle = "Mengikuti warna sistem (Material You)",
                    checked = s.dynamicColor,
                    onChange = vm::setDynamicColor,
                )
            }
        }

        SettingsSection(title = "Animasi") {
            SwitchRow(
                title = "Aktifkan animasi",
                subtitle = "Animasi halus di seluruh aplikasi",
                checked = s.animationsEnabled,
                onChange = vm::setAnimationsEnabled,
            )
            Spacer(Modifier.height(4.dp))
            SliderRow(
                title = "Kecepatan animasi",
                valueLabel = "%.2f×".format(s.animationSpeed),
                value = s.animationSpeed,
                range = 0.5f..2f,
                steps = 5,
                enabled = s.animationsEnabled,
                onChange = vm::setAnimationSpeed,
            )
            Spacer(Modifier.height(4.dp))
            LabeledRow(title = "Gaya masuk grid", enabled = s.animationsEnabled)
            ChipRow(
                options = listOf("Fade", "Zoom", "Cascade"),
                selectedIndex = s.gridEntrance.ordinal,
                enabled = s.animationsEnabled,
                onSelect = { vm.setGridEntrance(GridEntrance.entries[it]) },
            )
        }

        SettingsSection(title = "Ukuran & kartu") {
            SliderRow(
                title = "Jumlah kolom",
                valueLabel = s.columns.toString(),
                value = s.columns.toFloat(),
                range = 2f..5f,
                steps = 2,
                onChange = { vm.setColumns(it.roundToInt()) },
            )
            Spacer(Modifier.height(4.dp))
            SliderRow(
                title = "Sudut kartu",
                valueLabel = "${s.cornerRadiusDp} dp",
                value = s.cornerRadiusDp.toFloat(),
                range = 0f..28f,
                steps = 6,
                onChange = { vm.setCornerRadius(it.roundToInt()) },
            )
            Spacer(Modifier.height(4.dp))
            SliderRow(
                title = "Jarak antar kartu",
                valueLabel = "${s.spacingDp} dp",
                value = s.spacingDp.toFloat(),
                range = 0f..14f,
                steps = 6,
                onChange = { vm.setSpacing(it.roundToInt()) },
            )
            Spacer(Modifier.height(8.dp))
            LabeledRow(title = "Rasio thumbnail")
            ChipRow(
                options = listOf("Kotak", "Potret", "Asli"),
                selectedIndex = s.thumbRatio.ordinal,
                onSelect = { vm.setThumbRatio(ThumbRatio.entries[it]) },
            )
        }

        SettingsSection(title = "Lainnya") {
            LabeledRow(title = "Urutkan")
            ChipRow(
                options = listOf("Terbaru", "Terlama", "Nama A-Z", "Nama Z-A", "Ukuran"),
                selectedIndex = s.sort.ordinal.coerceIn(0, 4),
                onSelect = { vm.setSort(MediaSort.entries[it]) },
            )
            Spacer(Modifier.height(10.dp))
            SwitchRow(
                title = "Badge durasi video",
                checked = s.showDurationBadge,
                onChange = vm::setShowDurationBadge,
            )
            SwitchRow(
                title = "Bintang favorit di kartu",
                checked = s.showFavoriteStar,
                onChange = vm::setShowFavoriteStar,
            )
            SwitchRow(
                title = "PIP otomatis",
                subtitle = "Masuk PIP saat menekan Home ketika media sedang diputar",
                checked = s.autoPip,
                onChange = vm::setAutoPip,
            )
            SwitchRow(
                title = "Konfirmasi sebelum hapus",
                checked = s.confirmDelete,
                onChange = vm::setConfirmDelete,
            )
            SwitchRow(
                title = "Loop video",
                subtitle = "Video diputar berulang otomatis",
                checked = s.loopVideos,
                onChange = vm::setLoopVideos,
            )
            SwitchRow(
                title = "Pratinjau gerak di grid",
                subtitle = "Video bergerak sendiri di grid (matikan untuk menghemat baterai)",
                checked = s.animatedPreviews,
                onChange = vm::setAnimatedPreviews,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Reset setelan",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = { showResetDialog = true }) { Text("Reset") }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "galery ats • All The Storage",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset setelan?") },
            text = { Text("Semua pengaturan tampilan kembali ke bawaan. Daftar favorit tetap disimpan.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetSettings()
                    showResetDialog = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Batal") }
            },
        )
    }
}

// ---- Komponen kecil ---------------------------------------------------------

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp),
    )
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            content()
        }
    }
}

@Composable
private fun LabeledRow(title: String, enabled: Boolean = true) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = if (enabled) MaterialTheme.colorScheme.onBackground
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean = true,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, label ->
            FilterChip(
                selected = index == selectedIndex,
                enabled = enabled,
                onClick = { onSelect(index) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
    enabled: Boolean = true,
) {
    var local by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) { local = value }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = range,
            steps = steps,
            enabled = enabled,
        )
    }
}
