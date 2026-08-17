<div align="center">

# AlkaFish

### Pesca completa pra rede AlkaStudio — do zero ao AFK

Sistema de pesca com peixes por raridade/bioma/profundidade, varas evolutivas,
classes, torneios, sacola própria no banco e uma área pública de pesca AFK.

![Java](https://img.shields.io/badge/Java-21-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Version](https://img.shields.io/badge/Version-1.0.0-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

</div>

---

## 📋 Sobre o Projeto

O **AlkaFish** é o sistema de pesca da rede AlkaStudio: peixes com raridade,
peso e comprimento variáveis, varas que evoluem e podem quebrar, iscas,
classes de pesca com bônus próprios e uma área pública onde o jogador entra
via `/pescaria` (ou clicando num NPC) e pesca no modo AFK — a linha assenta
na água e o sistema pesca sozinho, contando os peixes na hotbar.

## ✨ Funcionalidades Principais

- 🐟 **Peixes por condição real**: bioma, profundidade, chuva e horário do
  dia decidem quais peixes podem aparecer, com peso/comprimento sorteados
  por raridade.
- 🎣 **Varas evolutivas**: suportam peso até um limite, podem quebrar (ou
  não) e dão recompensas extras por captura.
- 🧵 **Modo AFK**: entra na área pública, lança a vara e o sistema pesca
  sozinho enquanto o jogador segue online — sem precisar ficar clicando.
- 🎒 **Sacola própria no banco**: peixe pescado nunca ocupa espaço no
  inventário, fica salvo direto no banco de dados.
- 🏆 **Torneios** de pesca configuráveis, com ranking e recompensas.
- 🎽 **Classes de pesca**: armaduras equipáveis com bônus próprios
  (chance de peixe melhor, multiplicador de moeda, etc).
- 🎁 **Iscas** que ativam bônus temporários de sorte ao lançar a linha.
- 🎣 **Sistema de encantamentos** próprio pra vara, sem depender de
  encantamento vanilla.

## 🎮 Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/pescaria` (alias `/fish`, `/pesca`) | Abre o menu de pesca / teleporta pra área | `alkafish.use` |
| `/sair` | Sai da área de pesca | `alkafish.use` |
| `/alkafish` (alias `/alkapesca`) | Comandos administrativos | `alkafish.admin` |

## 🔗 Integrações

Depende de **AlkaCore** e **AlkaEconomy**. Integra com **AlkaShop** (venda),
**AlkaDrop**, **AlkaRankUp**, **AlkaVips** (bônus de VIP), **AlkaClans**,
**AlkaTime**, **LuckPerms**, **WorldGuard**, **Citizens** (NPC de acesso),
**DecentHolograms**, **mcMMO** e **ItemsAdder** — todas softdepend,
opcionais.

## 🔧 Tecnologias Utilizadas

- **Java 21** · **Paper API 1.21.8** (Folia-ready)
- **AlkaCore**: banco de dados e GUI compartilhados

## ⚙️ Instalação

1. Coloque `AlkaFish.jar` na pasta `plugins/` do servidor.
2. Certifique-se de ter **AlkaCore** e **AlkaEconomy** instalados (obrigatórios).
3. Reinicie o servidor.
4. Configure a área pública de pesca com os comandos administrativos.

## 🔐 Permissões

| Permissão | Descrição | Padrão |
|---|---|---|
| `alkafish.use` | Usar o sistema de pesca | ✅ |
| `alkafish.admin` | Acesso administrativo total | op |
| `alkafish.tournament.start` | Iniciar torneios manualmente | op |
| `alkafish.rod.upgrade` | Upar a vara de pesca | ✅ |
| `alkafish.rod.repair` | Reparar a vara de pesca | ✅ |
| `alkafish.class.equip` | Equipar classes de armadura | ✅ |
| `alkafish.class.upgrade` | Evoluir classes de armadura | ✅ |
| `alkafish.autosell` | Usar auto-venda de peixes | ❌ |

## 📝 Licença

> ⚠️ **Projeto proprietário da AlkaStudio.**
>
> Código fonte destinado exclusivamente ao uso interno da rede `Alka*`.
> Reprodução, distribuição ou uso não autorizado não são permitidos.

## 🎯 Créditos

- **Desenvolvido por**: MestreDEV — AlkaStudio
- **Parte do ecossistema**: `Alka*`

---

<div align="center">

**Desenvolvido com ❤️ pela AlkaStudio**

[![AlkaStudio](https://img.shields.io/badge/AlkaStudio-JLob0-blue)](https://github.com/JLob0)

</div>
