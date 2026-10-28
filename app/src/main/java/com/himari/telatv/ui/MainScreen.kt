package com.himari.telatv.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.himari.telatv.R
import com.himari.telatv.Shortcuts
import com.himari.telatv.TvViewModel
import com.himari.telatv.UiState
import com.himari.telatv.data.RemoteCommand
import kotlinx.coroutines.delay

@Composable
fun MainScreen(vm: TvViewModel) {
    val ui = vm.ui
    var hostText by rememberSaveable { mutableStateOf(ui.host) }

    // Primeiro uso: já procura a TV sozinho.
    LaunchedEffect(Unit) { if (!ui.paired && ui.host.isBlank()) vm.discover() }
    // Achou exatamente uma TV e o campo está vazio: preenche.
    LaunchedEffect(ui.found) { if (hostText.isBlank() && ui.found.size == 1) hostText = ui.found[0] }

    // Sem TV configurada, já abre nas configurações; com ela, a tela principal só tem os controles.
    var showSettings by rememberSaveable { mutableStateOf(!ui.ready) }
    BackHandler(enabled = showSettings && ui.ready) { showSettings = false }

    Scaffold { inner ->
        Column(
            Modifier
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("TelaTV", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = { showSettings = !showSettings }) {
                    if (!showSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (showSettings) "Fechar" else "Configurações")
                }
            }

            ui.message?.let { msg ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(msg, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (showSettings) {
                SetupCard(vm, ui, hostText) { hostText = it }
            } else if (ui.ready) {
                RemoteCard(vm, ui)
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Nenhuma TV configurada ainda.", style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { showSettings = true }) { Text("Abrir configurações") }
                    }
                }
            }
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
            val icon = if (ui.screenOff == true) R.drawable.ic_tv else R.drawable.ic_tv_off
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleLarge)
        }
    }

    // Direcionais: Voltar | ↑ | Início, ← | Confirmar | →, ↓ (segurar repete).
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RemoteButton(RemoteCommand.BACK, R.drawable.ic_arrow_back, vm)
            RemoteButton(RemoteCommand.UP, R.drawable.ic_keyboard_arrow_up, vm)
            RemoteButton(RemoteCommand.HOME, R.drawable.ic_home, vm)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RemoteButton(RemoteCommand.LEFT, R.drawable.ic_keyboard_arrow_left, vm)
            RemoteButton(RemoteCommand.OK, R.drawable.ic_check, vm, primary = true)
            RemoteButton(RemoteCommand.RIGHT, R.drawable.ic_keyboard_arrow_right, vm)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.weight(1f))
            RemoteButton(RemoteCommand.DOWN, R.drawable.ic_keyboard_arrow_down, vm)
            Spacer(Modifier.weight(1f))
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RemoteButton(RemoteCommand.VOLUME_DOWN, R.drawable.ic_volume_down, vm)
        RemoteButton(RemoteCommand.MUTE, R.drawable.ic_volume_off, vm)
        RemoteButton(RemoteCommand.VOLUME_UP, R.drawable.ic_volume_up, vm)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RemoteButton(RemoteCommand.PREVIOUS, R.drawable.ic_skip_previous, vm)
        RemoteButton(RemoteCommand.PLAY, R.drawable.ic_play_arrow, vm)
        RemoteButton(RemoteCommand.PAUSE, R.drawable.ic_pause, vm)
        RemoteButton(RemoteCommand.NEXT, R.drawable.ic_skip_next, vm)
    }
}

/**
 * Botão do controle com ícone Material. Comandos [RemoteCommand.repeatable] disparam ao tocar e
 * repetem enquanto o dedo segue pressionado; os demais disparam uma vez por toque.
 */
@Composable
private fun RowScope.RemoteButton(cmd: RemoteCommand, iconRes: Int, vm: TvViewModel, primary: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    if (cmd.repeatable) {
        LaunchedEffect(pressed) {
            if (pressed) {
                vm.send(cmd)
                delay(450)
                while (true) {
                    vm.send(cmd)
                    delay(130)
                }
            }
        }
    }
    val onClick: () -> Unit = if (cmd.repeatable) ({}) else ({ vm.send(cmd) })
    val modifier = Modifier.weight(1f).height(64.dp)
    val padding = PaddingValues(0.dp)
    val icon: @Composable () -> Unit = {
        Icon(painterResource(iconRes), contentDescription = cmd.label, modifier = Modifier.size(28.dp))
    }
    if (primary) {
        Button(onClick = onClick, modifier = modifier, contentPadding = padding, interactionSource = interaction) { icon() }
    } else {
        FilledTonalButton(onClick = onClick, modifier = modifier, contentPadding = padding, interactionSource = interaction) { icon() }
    }
}

@Composable
private fun SetupCard(vm: TvViewModel, ui: UiState, hostText: String, onHost: (String) -> Unit) {
    val ctx = LocalContext.current
    val pickKey = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = try {
                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            } catch (e: Exception) {
                null
            }
            if (text == null || text.length > 16_000) {
                Toast.makeText(ctx, "Não consegui ler esse arquivo como chave.", Toast.LENGTH_LONG).show()
            } else {
                vm.setSshKey(text)
            }
        }
    }
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
                Text("Controlar a música pelo celular (play, pausa, próxima, anterior)", Modifier.weight(1f))
                Switch(checked = ui.musicOnPhone, onCheckedChange = vm::setMusicOnPhone)
            }
            Text(
                "Por padrão, próxima/anterior usam as teclas CH+/CH− da TV. Se não pularem a faixa, ligue esta opção: " +
                    "ela usa as teclas de mídia do celular e funciona quando o Spotify do celular está controlando a música da TV (Spotify Connect).",
                style = MaterialTheme.typography.bodySmall
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Apagar a tela por SSH (TV com acesso root)", Modifier.weight(1f))
                Switch(checked = ui.sshEnabled, onCheckedChange = vm::setSshEnabled)
            }
            if (ui.sshEnabled) {
                Text(
                    "TV com root: não precisa parear. O SSH do Homebrew Channel usa a porta 22 e o usuário root. " +
                        "Entre com a chave privada abaixo ou, sem chave, com a senha (vazia ou alpine em TV recém-rootada).",
                    style = MaterialTheme.typography.bodySmall
                )
                if (!ui.paired) {
                    Button(onClick = { vm.saveHost(hostText) }, enabled = !ui.busy) { Text("Salvar IP (sem parear)") }
                }
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
                OutlinedTextField(
                    value = ui.sshKey,
                    onValueChange = vm::setSshKey,
                    label = { Text("Chave privada SSH (opcional)") },
                    supportingText = {
                        Text(if (ui.sshKey.isBlank()) "Cole o arquivo inteiro (-----BEGIN ... END-----), sem senha." else "Chave salva (${ui.sshKey.trim().length} caracteres).")
                    },
                    minLines = 2,
                    maxLines = 4,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { pickKey.launch(arrayOf("*/*")) }, enabled = !ui.busy) { Text("Importar arquivo") }
                    if (ui.sshKey.isNotBlank()) {
                        OutlinedButton(onClick = { vm.setSshKey("") }, enabled = !ui.busy) { Text("Remover chave") }
                    }
                }
                OutlinedButton(
                    onClick = { if (vm.saveHost(hostText)) vm.testSsh() },
                    enabled = !ui.busy
                ) { Text("Testar SSH") }
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
