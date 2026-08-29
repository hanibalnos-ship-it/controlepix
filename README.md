# Controle Pix — MVP pessoal

Aplicativo Android para uso pessoal que registra **Pix recebidos** a partir das notificações dos apps bancários e calcula totais automaticamente.

## O que já funciona

- Leitura de notificações via `NotificationListenerService`.
- Filtro: só tenta registrar notificações que contenham `Pix`, indícios de recebimento e valor em `R$`/`BRL`.
- Proteção contra duplicidade usando a chave da notificação Android.
- Total de hoje.
- Quantidade de recebimentos de hoje.
- Ticket médio de hoje.
- Total do mês.
- Histórico local dos últimos 250 lançamentos.
- Lançamento manual.
- Exclusão de lançamento.
- Banco de dados SQLite somente no aparelho.
- **Sem permissão INTERNET** no Manifest.

## Como abrir

1. Abra o Android Studio atualizado.
2. Use **File > Open** e escolha a pasta `ControlePix`.
3. Deixe o Android Studio sincronizar as dependências.
4. Se ele pedir a versão do Gradle, use **Gradle 9.5.1**.
5. Garanta que o SDK Android API 37 esteja instalado.
6. Conecte seu celular Android com depuração USB ou use um emulador.
7. Clique em **Run**.

## Primeiro uso no celular

1. Abra o app.
2. Toque em **Liberar acesso**.
3. Na tela do Android, habilite `Controle Pix` em acesso às notificações.
4. Volte ao app.
5. Faça/receba um Pix de teste pequeno ou use o botão `+` para testar o histórico.

## Importante para o primeiro teste real

Cada banco escreve a notificação de um jeito. O parser inicial cobre frases genéricas como:

- `Você recebeu um Pix de R$ 150,00`
- `Pix recebido R$ 150,00`
- `Entrada Pix de R$ 150,00`
- `Transferência Pix recebida de R$ 150,00`

E tenta ignorar mensagens como:

- `Pix enviado`
- `Você enviou`
- `Pagamento realizado`
- `Pix agendado`

Se o seu banco enviar uma frase diferente, basta ajustar `PixNotificationParser.kt`.

## Onde ajustar os bancos

Arquivo:

`app/src/main/java/br/com/controlepix/notification/PixNotificationParser.kt`

A função `bankNameFromPackage()` transforma o identificador do app em nomes como Nubank, Itaú, Inter etc.

## Privacidade

A permissão de acesso às notificações é ampla por regra do Android. Porém, este projeto:

- não declara permissão INTERNET;
- não envia dados para servidor;
- descarta notificações que não atendem aos sinais de Pix recebido;
- salva os registros em SQLite local.

Este MVP é um controle pessoal. Não deve ser usado como confirmação antifraude de pagamento sem conferência no banco.
