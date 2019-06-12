import os._
def move(from: Path, to: Path): Unit = {
  print(s"Moving ${from.relativeTo(os.pwd)} to ${to.relativeTo(os.pwd)}... ")
  os.move(from, to)
  println("Success!")
}

def remove(target: Path): Unit = {
  print(s"Removing ${target.relativeTo(os.pwd)}... ")
  os.remove(target)
  println("Success")
}
