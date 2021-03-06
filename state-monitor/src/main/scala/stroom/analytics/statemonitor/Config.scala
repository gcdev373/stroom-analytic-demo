package stroom.analytics.statemonitor

case class TagVals (tag1: String = null, tag2 : String = null, tag3: String = null)

case class Config(bootstrapServers: String, schemaFile: String, topic: String,
                  alertingDelay: String, alertFile: String,
                  tags : TagVals, states : Seq [State],
                  eventId: String = null, streamId: String = null, stroomAnnotationURL: String = null,
                  interval : String = null)

case class State (name : String, maxlatency : String, open: Transition, close: Transition = null)

case class Transition (filter: String, tags : Seq [Tag], requires : Seq [String] = Nil, timeout: String = null,
                       multipleMatchStrategy: String = null)

case class Tag (name: String, definition: String)

