# VoxShop (Fabric, Minecraft Java 26.2)

Tienda estilo skyblock: menú de cofre con categorías, compra y venta, y economía propia.

## Compilar
Requiere **JDK 25**.

    ./gradlew build

El `.jar` queda en `build/libs/voxshop-1.0.0.jar`. Copialo a `mods/` junto a **Fabric API** (0.150.1+26.2 o superior).
No hace falta instalarlo en los clientes: solo en el servidor (o en el mundo si juegas solo).

## Comandos
| Comando | Descripción |
|---|---|
| `/shop` | Abre la tienda |
| `/shop reload` | Recarga `shop.json` (admin) |
| `/balance`, `/bal` | Ver tu saldo |
| `/pay <jugador> <cantidad>` | Enviar dinero |
| `/eco give\|take\|set <jugador> <cantidad>` | Gestionar saldo (admin) |

## Uso del menú
- Clic izquierdo: comprar 1 — Shift + izquierdo: comprar 64
- Clic derecho: vender 1 — Shift + derecho: vender todo

## Configuración
Se genera sola en `config/voxshop/shop.json` la primera vez. Cada artículo es
`{ "item": "diamond", "buy": 400, "sell": 60 }`. Con `buy: -1` no se puede comprar y con `sell: -1` no se puede vender.
Los saldos se guardan en `config/voxshop/balances.json`.
