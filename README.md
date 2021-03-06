# type-formatter
Scala compiler plugin to format type errors

**This is a proof of concept and has not been tested against a large project. Use at your own risk.**

Note: This feature was added directly to the compiler by @VladUreche and is now available in the 2.12.2 https://github.com/scala/scala/pull/5589/files

This plugin hijacks the compiler's error reporting to improve the readability of symbolic infix types:

Before:

```scala
[error]  found   : shapeless.::[Int,shapeless.::[String,shapeless.::[Double,shapeless.HNil]]]
[error]  required: Int
[error]   val i: Int = 1 :: "a" :: 1.1 :: HNil
```

After:

```scala
[error]  found   : Int :: String :: Double :: shapeless.HNil
[error]  required: Int
[error]   val i: Int = 1 :: "a" :: 1.1 :: HNil
```

All the credit goes to Miles Sabin and Li Haoyi, since this is just gluing together code from [pprint]( https://github.com/lihaoyi/upickle-pprint/blob/c3227d34547fe974a47f74f537be4cf6eaefbc22/pprint/shared/src/main/scala-2.11/pprint/TPrintImpl.scala) and [si2712fix](https://github.com/milessabin/si2712fix-plugin/blob/5e25036f2353fed789520e55dd16284bd5982676/plugin/src/main/scala/si2712fix/Plugin.scala#L24
).

## TODO
* add annotation/option to allow non-symbolic types like Xor to be printed infix

