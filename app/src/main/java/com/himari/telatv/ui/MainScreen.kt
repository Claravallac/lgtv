package com.himari.telatv.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.himari.telatv.Shortcuts
import com.himari.telatv.TvViewModel
import com.himari.telatv.UiState
import com.himari.telatv.data.RemoteCommand

@Composable
fun MainScreen(vm: TvViewModel) {
    val ui = vm.ui
    var hostText by rememberSaveable { mutableStateOf(ui.host) }

    // Primeiro uso: já procura a TV sozinho.
    LaunchedEffect(Unit) { if (!ui.paired && ui.host.isBlank()) vm.discover() }
    // Achou exatamente uma TV e o campo está vazio: preenche.
    LaunchedEffect(ui.found) { if (hostText.isBlank() && ui.found.size == 1) hostText = ui.found[0] }

    Scaffold { inner ->
        Column(
            Modifier
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("TelaTV", style = MaterialTheme.typography.headlineMedium)

            ui.message?.let { msg ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(msg, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (ui.paired) RemoteCard(vm, ui)
            SetupCard(vm, ui, hostText) { hostText = it }
        }
    }
}

@Composable
private fun RemoteCard(vm: TvViewModel, ui: UiState) {
    val label = when (ui.screenOff) {
        true -> "Acender tela"
        false -> "Apagar tela"
        null -> "Apagar / acender tela"
    }
    Button(
        onClick = vm::toggleScreen,
        enabled = !ui.busy,
        modifier = Modifier.fillMaxWidth().height(96.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        if (ui.busy) {
            CircularProgressIndicator(Modifier.size(28.dp), color = LocalContentColor.current, strokeWidth = 3.dp)
        } else {
            Text(label, style = MaterialTheme.typography.titleLarge)
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RemoteButton("Vol −", RemoteCommand.VOLUME_DOWN, vm)
        RemoteButton("Mudo", RemoteCommand.MUTE, vm)
        RemoteButton("Vol +", RemoteCommand.VOLUME_UP, vm)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RemoteButton("⏮", RemoteCommand.PREVIOUS, vm)
        RemoteButton("▶", RemoteCommand.PLAY, vm)
        RemoteButton("⏸", RemoteCommand.PAUSE, vm)
        RemoteButton("⏭", RemoteCommand.NEXT, vm)
    }
}

@Composable
private fun RowScope.RemoteButton(text: String, cmd: RemoteCommand, vm: TvViewModel) {
    FilledTonalButton(
        onClick = { vm.send(cmd) },
        modifier = Modifier
            .weight(1f)
            .height(64.dp)
            .semantics { contentDescription = cmd.label }
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SetupCard(vm: TvViewModel, ui: UiState, hostText: String, onHost: (String) -> Unit) {
    val ctx = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (ui.paired) "Configuração" else "Primeiro uso",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedButton(onClick = vm::discover, enabled = !ui.searching && !ui.busy) {
                if (ui.searching) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Procurando a TV…")
                } else {
                    Text("Buscar TV na rede")
                }
            }
            ui.found.forEach { ip ->
                FilledTonalButton(onClick = { onHost(ip); vm.pair(ip) }, enabled = !ui.busy) {
                    Text("Parear com a TV LG $ip")
                }
            }
            OutlinedTextField(
                value = hostText,
                onValueChange = onHost,
                label = { Text("IP da TV (ou digite, se a busca não achar)") },
                supportingText = { Text("Na TV: Configurações › Rede (use IP fixo no roteador)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { vm.pair(hostText) }, enabled = !ui.busy) {
                Text(if (ui.paired) "Parear de novo" else "Parear")
            }
            Text(
                "Na TV, deixe ativado o controle pela rede (LG Connect Apps) e aceite o pedido que aparecer na tela.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Abrir o Spotify da TV ao apagar a tela", Modifier.weight(1f))
                Switch(checked = ui.spotifyOnTv, onCheckedChange = vm::setSpotifyOnTv)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Apagar a tela por SSH (TV com acesso root)", Modifier.weight(1f))
                Switch(checked = ui.sshEnabled, onCheckedChange = vm::setSshEnabled)
            }
            if (ui.sshEnabled) {
                OutlinedTextField(
                    value = ui.sshUser,
                    onValueChange = vm::setSshUser,
                    label = { Text("Usuário SSH") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ui.sshPort,
                    onValueChange = vm::setSshPort,
                    label = { Text("Porta SSH") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ui.sshPassword,
                    onValueChange = vm::setSshPassword,
                    label = { Text("Senha SSH (vazio se não tiver)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = vm::testSsh, enabled = !ui.busy) { Text("Testar SSH") }
            }
            val version = try {
                ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
            } catch (e: Exception) {
                null
            }
            if (version != null) Text("TelaTV v$version", style = MaterialTheme.typography.bodySmall)
            if (Shortcuts.canPin(ctx)) {
                OutlinedButton(onClick = {
                    val ok = Shortcuts.requestPin(ctx)
                    if (!ok) Toast.makeText(ctx, "Não consegui fixar o atalho neste launcher.", Toast.LENGTH_LONG).show()
                }) { Text("Fixar atalho na tela inicial") }
            }
        }
    }
}
