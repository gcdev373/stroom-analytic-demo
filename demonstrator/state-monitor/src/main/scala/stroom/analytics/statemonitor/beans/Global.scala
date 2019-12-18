package stroom.analytics.statemonitor.beans

case class TagVals (tag1: String = null, tag2 : String = null, tag3: String = null)

case class Global(bootstrapServers: String, schemaFile: String, topic: String, tags : TagVals, states : Seq [State] )

case class State (name : String, maxlatency : String, open: Transition, close: Transition = null)

case class Transition (filter: String, timeout: String = "P1D", tags : Seq [Tag], requires : Seq [String] = Nil)

case class Tag (name: String, definition: String)

