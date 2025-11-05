package us.awfl.yaml

import us.awfl.dsl.*

object Constants {
  def replace[T](value: Resolved[T]): Resolved[T] = value match {
    case WORKFLOW_ID => str(CelFunc("sys.get_env", "GOOGLE_CLOUD_WORKFLOW_ID"))
    case WORKFLOW_EXECUTION_ID => str(CelFunc("sys.get_env", "GOOGLE_CLOUD_WORKFLOW_EXECUTION_ID"))
    case PROJECT_ID => str(CelFunc("sys.get_env", "GOOGLE_CLOUD_PROJECT_ID"))
    case LOCATION => str(CelFunc("sys.get_env", "GOOGLE_CLOUD_LOCATION"))
    case _ => value
  }
}