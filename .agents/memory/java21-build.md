---
name: Java 21 build setup
description: How to compile the NuclearCraft Maven plugin when Nix only has JDK 16.
---

## Rule
JDK 16 is the highest version available via `nix-env -iA nixpkgs.jdk`. The Paper API (1.21.4) targets Java 21, so compiling against it with JDK 16 produces "cannot access org.bukkit.*" errors on every class. You must use JDK 21.

## How to apply
Download Adoptium JDK 21 to /tmp:
```
curl -sL "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.11%2B10/OpenJDK21U-jdk_x64_linux_hotspot_21.0.11_10.tar.gz" -o /tmp/jdk21.tar.gz
tar xzf /tmp/jdk21.tar.gz -C /tmp
export JAVA_HOME=/tmp/jdk-21.0.11+10
export PATH="$HOME/.nix-profile/bin:$JAVA_HOME/bin:$PATH"
```
Then run: `cd nuclearcraft-plugin && mvn package -q -DskipTests`

Maven 3.8.1 is installed via `nix-env -iA nixpkgs.maven`.
The Paper API SNAPSHOT jar is cached at: ~/.m2/repository/io/papermc/paper/paper-api/1.21.4-R0.1-SNAPSHOT/

**Why:** Nix channel (nixos-22.11 era) has jdk/jdk16 but not jdk21. Paper API compiled with JDK 21 cannot be read by JDK 16 compiler.
