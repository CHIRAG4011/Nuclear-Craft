---
name: NCLogger API
description: NCLogger.info/warn/debug only accept a single String — no printf-style varargs.
---

## Rule
`NCLogger.info()`, `NCLogger.warn()`, `NCLogger.debug()` all take a single `String` argument. Passing multiple arguments causes a compile error.

## Why
NCLogger is a thin wrapper around Bukkit's logger — it does not use Java's `java.util.logging.Logger` varargs overloads.

## How to apply
Use `String.format()` at the call site:

```java
// WRONG — won't compile
NCLogger.info("Player %s reached milestone %s.", player.getName(), milestone.name());

// CORRECT
NCLogger.info(String.format("Player %s reached milestone %s.", player.getName(), milestone.name()));
```

Note: `NCLogger.debug()` DOES accept printf-style varargs (it wraps String.format internally).
Check which overloads exist before using. When in doubt, use String.format().
