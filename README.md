# TelaTV

Controle remoto mínimo para ouvir música (Spotify da TV) com a tela apagada, em TV LG webOS:
apagar/acender a tela, volume, mudo, play/pausa, próxima/anterior. Atalho no ícone e botão nas
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
