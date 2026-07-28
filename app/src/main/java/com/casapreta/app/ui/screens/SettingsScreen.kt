package com.casapreta.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.casapreta.app.viewmodel.SettingsViewModel
import com.casapreta.app.shizuku.ShizukuManager

@Composable
fun SettingsScreen(
    onNavigateHome: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val shizukuStatus by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val shizukuPermission by viewModel.shizukuPermission.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))

        // ===== APARÊNCIA / MODO NOTURNO =====
        SectionHeaderCard("Aparência", Icons.Filled.BrightnessLow)

        // Modo Noturno switch (single source of truth: themeMode == DARK)
        CardRow(
            icon = Icons.Filled.DarkMode,
            title = "Modo Noturno",
            subtitle = if (themeMode.name == "DARK")
                "Ativado — tema escuro em uso"
            else
                "Toque para ativar o tema escuro",
            trailing = {
                androidx.compose.material3.Switch(
                    checked = themeMode.name == "DARK",
                    onCheckedChange = { isChecked ->
                        viewModel.setThemeMode(
                            if (isChecked) com.casapreta.app.ui.theme.ThemeMode.DARK
                            else com.casapreta.app.ui.theme.ThemeMode.LIGHT
                        )
                    }
                )
            }
        )

        // Three-way choice (System / Light / Dark) — visible as buttons
        ThemeModeSelector(
            current = themeMode,
            onSelect = { viewModel.setThemeMode(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ===== SHIZUKU =====
        SectionHeaderCard("Integração Shizuku", Icons.Filled.Shield)

        val statusText = when (shizukuStatus) {
            ShizukuManager.Status.NotInstalled -> "Shizuku não encontrado"
            ShizukuManager.Status.NotRunning -> "Shizuku instalado — servidor parado"
            ShizukuManager.Status.Running -> if (shizukuPermission)
                "Conectado e autorizado"
            else
                "Conectado — permissão pendente"
        }

        CardRow(
            icon = Icons.Filled.Shield,
            title = "Status do Shizuku",
            subtitle = statusText,
            trailing = {
                Icon(
                    imageVector = if (shizukuStatus == ShizukuManager.Status.Running && shizukuPermission)
                        Icons.Filled.Download else Icons.Filled.Shield,
                    contentDescription = null,
                    tint = if (shizukuStatus == ShizukuManager.Status.Running && shizukuPermission)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }
        )

        if (shizukuStatus == ShizukuManager.Status.Running && !shizukuPermission) {
            Button(
                onClick = { viewModel.requestShizukuPermission() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Autorizar Shizuku")
            }
        }

        OutlinedButton(
            onClick = { viewModel.refreshShizukuState() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verificar novamente")
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onNavigateHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Voltar ao início")
        }
    }
}

@Composable
private fun SectionHeaderCard(text: String, icon: ImageVector) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CardRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            trailing()
        }
    }
}

@Composable
private fun ThemeModeSelector(
    current: com.casapreta.app.ui.theme.ThemeMode,
    onSelect: (com.casapreta.app.ui.theme.ThemeMode) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            com.casapreta.app.ui.theme.ThemeMode.SYSTEM to "Sistema",
            com.casapreta.app.ui.theme.ThemeMode.LIGHT to "Claro",
            com.casapreta.app.ui.theme.ThemeMode.DARK to "Escuro"
        ).forEach { (mode, label) ->
            val selected = current == mode
            androidx.compose.material3.FilterChip(
                selected = selected,
                onClick = { onSelect(mode) },
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
