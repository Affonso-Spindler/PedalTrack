# PedalTrack

App Android pessoal para registrar sessões de ciclismo indoor, lendo os treinos sincronizados do Galaxy Watch via Samsung Health.

## Setup

### 1. Pré-requisitos

- JDK 17
- Android SDK (`compileSdk 34`, `minSdk 29`)
- Um dispositivo com Samsung Health instalado e "Modo desenvolvedor (Samsung Health Data SDK)" ativado com leitura de dados habilitada (Samsung Health → ⋮ → Configurações → Sobre o Samsung Health → toque 10x na versão)

### 2. Samsung Health Data SDK

O app depende do **Samsung Health Data SDK**, que não está disponível no Maven Central — precisa ser baixado manualmente:

1. Acesse [developer.samsung.com/health/data](https://developer.samsung.com/health/data) (login com conta Samsung)
2. Baixe o pacote do SDK (zip)
3. Dentro do zip, copie o arquivo `libs/samsung-health-data-api-<versão>.aar`
4. Cole em `app/libs/samsung-health-data-api-1.1.0.aar` neste projeto (a pasta `app/libs/` já existe; o `.aar` é ignorado pelo git, então precisa ser colocado manualmente a cada clone novo)

Se a versão baixada for diferente de `1.1.0`, ajuste o nome do arquivo referenciado em `app/build.gradle.kts` (`implementation(files("libs/samsung-health-data-api-<versão>.aar"))`).

### 3. Build

```bash
./gradlew assembleDebug
```

## Testes

```bash
./gradlew test
```
