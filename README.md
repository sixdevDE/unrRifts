# unrRifts (Paper 1.21.10)

**Build**
- Java 21, Maven 3.9.11
- `mvn -DskipTests package`

**Requires**
- Multiverse-Core (plugin.yml depend)

**Setup**
1. Create/import your lobby world with Multiverse (example world name: `lobby`)
2. Start server, then:
   - `/unrrifts setlobby` in your lobby to set spawn
   - `/unrrifts setexit` to set exit location
3. Players use `/rift` to queue.

**Custom Maps**
- Put your template world folder in server root (same as other world folders)
- Register:
  - `/unrrifts map create <name> <worldFolder>`
  - set hub/boss/exfil/spawns
  - `/unrrifts map enable <name> true`

Generated runs build structures around (0, buildY, 0) and are created only when the lobby is ready.
