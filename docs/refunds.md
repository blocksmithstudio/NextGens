# Pending generator delivery

Admin gives, `/pickupgens`, generator-view GUI pickups, shop purchases, first-join generators, and normal generator pickup with `drop-on-break: false` share the pending delivery system. They insert what fits immediately and save only the inventory API's returned leftovers. A successful delivery does not create a pending record or write refund data. Shop purchases still charge the configured price, with undelivered purchases safely pending.

`drop-on-break: true` deliberately drops the generator at its block location, even when the player has room. This option bypasses pending delivery. There is no ground-item interception: intentional drops and unrelated dropped items are untouched.

With `island-pickup: true`, SuperiorSkyblock2 and BentoBox return removed generators through the existing refund queue for both online and offline owners. Refunds created through `GeneratorAPI.giveGenerator` use the same queue.

NextGens tries to deliver queued generators on login and when a refund is queued, then retries online players every second. It fills matching stacks and available storage slots. Anything that does not fit remains under `items.<player UUID>` in `data.yml`, using the existing list of generator IDs. No refund item is dropped on the ground. Large refunds can be delivered in several batches.

Online players with undelivered items receive an immediate reminder, then at most one reminder per minute while inventory space is still insufficient. Freeing space allows the next automatic check to deliver more items. There is no claim command.

Configure the reminder in `config.yml`:

```yaml
messages:
  pending-refund: '&6[NextGens] &aYou have pending generators to be given to you: &f{generators}&r&a, free up at least &f{slots} &aslots in your inventory to receive the generators.'
```

- `{generators}` groups the remaining items by generator ID and includes each count and its configured colored `display-name`.
- `{slots}` is the number of additional storage slots needed for the remaining items, respecting item and inventory stack limits. Armor and offhand slots are not counted.

For example, with a full inventory, two stackable wheat generators and one pumpkin generator need two slots. If a wheat stack already has room for both wheat generators, those are delivered and the reminder lists only one pumpkin generator needing one slot. Sixty-five generators with a stack limit of 64 need two slots. Custom stack limits are respected.

The normal config updater supplies the new message to existing installations. If automatic config updates are disabled and the key is absent, the built-in default still sends reminders; add the key manually to customize it. `/nextgens reload` picks up message edits without clearing the refund queue. Invalid/missing generator definitions are retained and logged instead of discarded; restoring the definition lets automatic delivery resume.

Delivery, queue updates and refund saves are serialized; only successfully inserted quantities are removed. Polling stops during plugin shutdown. Immediate delivery checks the Bukkit main thread or Folia entity ownership first. Requests made off the owning thread, after disconnect, or during shutdown are saved in full before scheduled delivery, so a cancelled or retired task cannot lose them. On Folia, queued inventory work is dispatched to the player's owning entity scheduler. The existing MDLib provider remains in use for polling and Bukkit scheduling; Folia ownership and entity calls use narrow reflective bridges because NextGens compiles against Spigot and MDLib 2.4.1's player timer passes its runnable as the retirement callback.

## Verification

Run `./mvnw.cmd -B -ntp clean verify` and `java scripts/RefundCheck.java` with Java 25. The dependency-free regression harness compiles the actual refund manager, refund join listener, both island listeners, and all six delivery paths against small API substitutes. It exercises full and available inventories through commands and GUI callbacks, first-join grants, both break modes and drop settings, shop charging, overflow-only persistence, partial stacks, custom stack limits, reminder timing/configuration, saved refunds, disconnects, shutdown, scheduler retirement, concurrent queue additions, and island removal handlers. These checks verify the offline behavior; they do not claim a live Minecraft or Folia session.

Inventory behavior follows the [Paper inventory API contract](https://jd.papermc.io/paper/1.21.11/org/bukkit/inventory/Inventory.html#addItem(org.bukkit.inventory.ItemStack...)); thread checks use [entity ownership](https://jd.papermc.io/paper/1.21.11/org/bukkit/Bukkit.html#isOwnedByCurrentRegion(org.bukkit.entity.Entity)), and region dispatch follows the [entity scheduler contract](https://jd.papermc.io/paper/1.21.8/io/papermc/paper/threadedregions/scheduler/EntityScheduler.html).
