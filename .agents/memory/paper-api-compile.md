---
name: Paper API compile quirk
description: Why compiling with JDK < 21 gives "cannot access org.bukkit.*" errors.
---

## Rule
The Paper API jar for 1.21.4 is compiled targeting Java 21 (class file version 65). When javac 16 tries to read these class files, it reports "cannot access org.bukkit.Foo" for every Bukkit class — it looks like a classpath problem but is actually a class file version mismatch.

**Why:** Java compilers refuse to process class files compiled for a higher JVM version.

## How to apply
Always use JDK 21 (see java21-build.md) when building this plugin. Never attempt to lower `maven.compiler.release` below 21 as a workaround — it won't work.

## Pre-existing bug fixed
`RadiationVisualManager.java` line 136-139 called `.floatValue()` on a primitive `double` returned by `RandomUtil.nextDouble()`. Fixed to `(float) RandomUtil.nextDouble(...)`.
