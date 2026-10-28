# TelaTV

Controle remoto mínimo para ouvir música (Spotify da TV) com a tela apagada, em TV LG webOS:
apagar/acender a tela, YouTube/Spotify, direcionais/confirmar/voltar/início, volume, mudo, play/pausa, próxima/anterior. Atalho no ícone e botão nas
Configurações Rápidas alternam a tela com um toque.

## Primeiro uso
1. Na TV: ative o controle pela rede (LG Connect Apps) e reserve um IP fixo no roteador.
2. No app: digite o IP da TV e toque em **Parear**; aceite o pedido na tela da TV.
3. Use o botão grande, ou fixe o atalho / adicione o botão "Tela da TV" nas Configurações Rápidas.

### TV com root (recomendado se a sua tem Homebrew Channel)
Não precisa parear. Em Configuração, ligue **Apagar a tela por SSH**: porta **22**, usuário **root**, e a
**chave privada** (cole o arquivo inteiro ou importe; sem senha, RSA: `ssh-keygen -t rsa -b 2048 -m PEM`).
Sem chave, deixe a senha vazia ou tente `alpine`. Salve o IP, toque em **Testar SSH** e use o botão grande.
A 9922 (Modo Desenvolvedor, usuário prisoner) e a 9998 (serviço do Homebrew) não servem para este app.

Veja `PLANO.md` para o estado do projeto. Build: GitHub Actions (`Build APK`), APK em Artifacts.

## Pular faixa (próxima/anterior) no Spotify da TV
A TV LG não tem comando de "pular faixa", então o app usa a API do Spotify (precisa de **Premium**):
1. Em <https://developer.spotify.com/dashboard> crie um app e marque **Web API**.
2. Em **Redirect URIs** coloque exatamente `telatv://spotify-callback` e salve.
3. No app, em Configuração, cole o **Client ID**, toque em **Conectar** e autorize no navegador.
4. Toque uma música no Spotify da TV; Próxima/Anterior passam a pular a faixa do player da TV.
O app só controla a TV: nada é enviado ao player do celular.
