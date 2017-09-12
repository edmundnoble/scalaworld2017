package sbench.examples

import cats.{Monad, MonadError, Show}
import cats.implicits._

object NoTParams {
  // extra note with regard to the power of abstractions:
  // you can get better performance for many usecases by including `has(key: String): Boolean` and
  // adding versions of `remove` and `put` returning the value previously at that key. but if you remove
  // the old versions of `remove` and `put`, you're going to waste network bandwidth in some usecases.
  trait KeyValueStore {
    def get(key: String): Option[String]

    def remove(key: String): Boolean

    def put(key: String, value: String): Boolean
  }

  def putIfAbsent(key: String, value: String)(implicit store: KeyValueStore): Boolean = {
    val got = store.get(key)
    got.fold(store.put(key, value))(_ => false)
  }

  def initProgram(implicit store: KeyValueStore): Unit = {
    putIfAbsent("initConfig", "{scalaworldconf: true}")
    assert(store.get("serialNumber").isDefined)
  }
}

object KVTParams {
  trait KeyValueStore[K, V] {
    def get(key: K): Option[V]

    def remove(key: K): Boolean

    def put(key: K, value: V): Boolean
  }

  def putIfAbsent[K, V](key: K, value: V)(implicit store: KeyValueStore[K, V]): Boolean = {
    val got = store.get(key)
    got.fold(store.put(key, value))(_ => false)
  }

  def initProgram(implicit store: KeyValueStore[String, String]): Unit = {
    putIfAbsent("initConfig", "{scalaworldconf: true}")
    assert(store.get("serialNumber").isDefined)
  }
}

object EffectKVTParams {
  trait KeyValueStore[F[_], K, V] {
    def get(key: K): F[Option[V]]
    def remove(key: K): F[Boolean]
    def put(key: K, value: V): F[Boolean]
  }

  def putIfAbsent[F[_]: Monad, K, V](key: K, value: V)(implicit store: KeyValueStore[F, K, V]): F[Boolean] = {
    for {
      got <- store.get(key)
      out <- got.fold(store.put(key, value))(_ => false.pure[F])
    } yield out
  }

  sealed trait ProgramInitError
  case object MissingSerialError extends ProgramInitError
  final def missingSerialError: ProgramInitError = MissingSerialError
  object ProgramInitError {
    implicit val showProgramInitError: Show[ProgramInitError] = Show.show {
      case MissingSerialError => s"Program configuration is missing serial key."
    }
  }

  def initProgram[F[_]: MonadError[?[_], ProgramInitError]](implicit store: KeyValueStore[F, String, String]): Unit = {
    for {
      _ <- putIfAbsent[F, String, String]("initConfig", "{scalaworldconf: true}")
      serial <- store.get("serialNumber")
      _ <- serial.fold(missingSerialError.raiseError[F, Unit])(_ => ().pure[F])
    } yield ()
  }

}
