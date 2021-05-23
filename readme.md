### Description
The online whiteboard, allows edit by several clients and keep in sync.

### Used Technologies
* **redis streams** - streaming data between users, initial storage
* **postgresql** - store snapshots of images, store user-related info
* **other** - redis4cats-streams, http4s, doobie, fs2, cats, circe
