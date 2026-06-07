---
name: Phase 12 resourcepack API
description: Paper 1.21 Player.setResourcePack() takes byte[] hash, not String. Argument order and hex conversion required.
---

## Rule
`Player.setResourcePack()` in Paper 1.21 does NOT accept a `String` hash. The hash must be a `byte[]`. The correct overload signature is:

```java
player.setResourcePack(String url, byte[] hash, Component prompt, boolean required);
```

## Why
Earlier drafts of ResourcePackManager passed the hex string directly. Paper 1.21 changed the API to enforce binary hashes. Passing a `String` in the hash position causes a compile error: "no suitable method found".

## How to apply
Convert the hex SHA-1 string (from config) to byte[] before calling:

```java
private static byte[] parseHex(String hex) {
    if (hex == null || hex.isBlank()) return new byte[0];
    int len = hex.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
        data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
    }
    return data;
}
```

Then call: `player.setResourcePack(url, parseHex(hashString), prompt, required);`
