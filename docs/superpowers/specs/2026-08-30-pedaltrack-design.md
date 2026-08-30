# PedalTrack — Design

Data: 2026-08-30

## Contexto e propósito

App Android pessoal (uso individual, sem multiusuário) para registrar sessões
de ciclismo indoor. O usuário treina numa bike indoor usando um Galaxy Watch 7,
que grava o treino no Samsung Health; essa sessão sincroniza automaticamente
para o Health Connect (Samsung Health é parceiro bidirecional do Health
Connect desde a v6.22.5, out/2022).

O app não faz tracking ao vivo nem depende de Bluetooth/sensores. O fluxo é
sempre pós-treino: o usuário abre o app, escolhe a sessão de ciclismo já
registrada pelo relógio, e completa o único dado que o relógio não sabe: a
quilometragem rodada na bike (carga/resistência é opcional e imprecisa, então
vira texto livre).

Calorias, frequência cardíaca e duração vêm sempre do Health Connect — nunca
são digitadas manualmente.

## Decisões de arquitetura

**Fonte de verdade dupla, com prioridade local.** O app escreve o km
informado como `DistanceRecord` no Health Connect (associado à sessão por
sobreposição de horário), permitindo que o Samsung Health e qualquer outro
app leitor do Health Connect também enxerguem o dado. Mas o app mantém seu
próprio banco local (Room) como fonte de verdade para as suas próprias
telas — isso porque há relatos conhecidos de o Samsung Health não ler de
volta, de forma confiável, dados escritos por apps terceiros no Health
Connect. Se a escrita no Health Connect falhar, o lançamento local não é
bloqueado; o erro de sync vira um aviso não bloqueante.

**Escopo da v1: só ciclismo indoor.** Outras atividades (corrida, musculação
etc.) ficam fora — cada tipo de atividade teria campos manuais diferentes, e
isso é decisão de uma v2, não desta.

## Stack técnica

- Kotlin + Jetpack Compose
- Room (banco local)
- Health Connect Client (`androidx.health.connect.client`) para leitura de
  sessões `EXERCISE_TYPE_STATIONARY_BIKE` e escrita de `DistanceRecord`
- Arquitetura MVVM (ViewModel + Repository)
- `minSdk` compatível com Health Connect (Android 9+; irrelevante na prática
  já que o dispositivo alvo é um Galaxy S25+)

## Modelo de dados

Tabela Room `cycling_sessions`:

| Campo | Tipo | Origem | Obrigatório |
|---|---|---|---|
| `id` | Long (PK, autogerado) | local | sim |
| `healthConnectSessionId` | String | Health Connect | sim (chave de dedup) |
| `startTime` | Instant | Health Connect | sim |
| `endTime` | Instant | Health Connect | sim |
| `durationMin` | Int | Health Connect (calculado) | sim |
| `calories` | Double? | Health Connect | não (pode faltar na origem) |
| `avgHeartRate` | Int? | Health Connect | não (pode faltar na origem) |
| `km` | Double | manual | sim |
| `carga` | String? | manual | não |
| `createdAt` | Instant | local (momento do lançamento) | sim |

`healthConnectSessionId` é único no banco local — usado tanto para dedup
(uma sessão só pode ser lançada uma vez) quanto para filtrar a lista de
sessões disponíveis pra lançar.

## Telas

### 1. Lançar exercício (tela inicial)

- Consulta o Health Connect por sessões `EXERCISE_TYPE_STATIONARY_BIKE` dos
  últimos 30 dias.
- Remove da lista qualquer sessão cujo `healthConnectSessionId` já exista em
  `cycling_sessions`.
- Lista ordenada da mais recente pra mais antiga, mostrando data, duração,
  calorias e FC média (quando disponíveis).
- Ao selecionar uma sessão: formulário com campo `km` (obrigatório, numérico)
  e `carga` (opcional, texto livre) → botão salvar.
- Salvar grava local (Room) e tenta escrever `DistanceRecord` no Health
  Connect no mesmo intervalo `startTime`–`endTime`. Falha na escrita do
  Health Connect não impede o salvamento local; mostra aviso não bloqueante.
- Estado vazio: "nenhum treino novo nos últimos 30 dias".
- Sem permissão do Health Connect concedida: tela explicando e pedindo pra
  conceder a permissão (leitura de exercício/distância + escrita de
  distância).

### 2. Histórico

- Lista as sessões já lançadas (da tabela local), mais recente primeiro.
- Cada item: data, duração, km, carga (se houver), calorias, FC média.
- Editar: reabre o formulário de km/carga para aquela sessão (não altera o
  vínculo com a sessão do Health Connect).
- Apagar: remove a linha do Room. **Não** remove o `DistanceRecord` já
  escrito no Health Connect (fora do escopo — o Health Connect tem sua
  própria tela de gestão de dados/permissões).

### 3. Resumo

- Filtro de período: semana atual, mês atual, tudo.
- Métricas: km total, km médio por sessão, calorias totais, tempo médio de
  sessão.
- Calculado sobre os dados da tabela local (`cycling_sessions`), não faz
  nova consulta ao Health Connect.

## Erros e casos de borda

- Permissão do Health Connect negada ou revogada depois: detectar no início
  de cada consulta e mostrar a tela de pedido de permissão em vez de lista
  vazia.
- App Health Connect não instalado no aparelho: irrelevante no caso de uso
  atual (Galaxy S25+ com Android recente já traz o serviço), mas se a API
  reportar indisponibilidade, mostrar mensagem explicando que é necessário
  instalar/atualizar o Health Connect.
- Sessão sem calorias ou FC média na origem (Health Connect): exibir campo
  como "—" em vez de falhar.
- Dupla escrita/duplicidade: dedup garantido pela constraint de unicidade em
  `healthConnectSessionId` na tabela local — tentar lançar a mesma sessão
  duas vezes falha silenciosamente (a sessão já não aparece na lista de
  "lançar exercício" depois da primeira vez).

## Testes

- Unitário: lógica de dedup/filtro de sessões (Repository), cálculo das
  métricas de resumo.
- Unitário: mapeamento de dados do Health Connect → entidade Room.
- Instrumentado (Android): fluxo de escrita/leitura do Room (DAO).
- Manual: fluxo ponta a ponta com o Health Connect real (não é praticamente
  mockável) — gravar um treino no relógio, confirmar que aparece na lista,
  lançar, confirmar no histórico e no resumo, e conferir que o
  `DistanceRecord` foi escrito no Health Connect (via app Health Connect,
  seção de dados).

## Fora de escopo (v1)

- Outras atividades além de ciclismo indoor.
- Tracking ao vivo / sensores Bluetooth / companion app Wear OS.
- Edição do `DistanceRecord` já escrito no Health Connect a partir do app.
- Sincronização em nuvem / multiusuário / backup remoto do banco local.
