object GitType extends Enumeration {
  type GitType = Value
  val Open , Internal = Value
}

GitType.withName("Open")
GitType.withName("Internal")