#!/bin/sh
exec amm "$0" "$@"
!#

import scala.sys.process._

@main
def main(args: String*): Unit = {
  val (options, fileStrings) = args.partition(_.startsWith("-"))
  val config = parseOptions(options)
  val files = fileStrings.map(x => os.FilePath(x))
  run(config, files)
}

case class Config(replaceStreams: Boolean = false)

def parseOptions(options: Seq[String]): Config = {
  var config = Config()
  options foreach {
    case "-h"|"--help" => {
      println("""Usage: audiotranscode [OPTION]... [FILE]...
     |Convert the audio tracks of the given files to stereo AAC streams, replacing the files with new matroska (.mkv) files.
     |By default the transcodes tracks are added as extra tracks, pass -t to replace the originals""".stripMargin)
      sys.exit()}
    case "-t" => config=config.copy(replaceStreams=true)
    case _ => println("Invalid args")
      sys.exit
  }
  config
}

def run(config: Config, files: Seq[os.FilePath]) {
  for (inPath <- files.map(_.toString)) {
    val base = inPath.replaceAll("\\.[^.]*$", "")
    val tempInPath = base + ".original-temp"
    val outPath = base + ".mkv"
    val tempOutPath = base + ".new-temp"
  
    Seq("mv", inPath, tempInPath).!
    val audioCount = audioStreams(tempInPath)
    //Create ffmpeg command, with audio mapped twice, once 
    //for trancode and once for copy
    val command = Seq("ffmpeg", "-y", "-nostats", "-hide_banner",
      "-fflags", "+genpts", "-i", tempInPath, "-map", "0", "-map", "0:a", 
      "-flags", "+global_header", "-codec", "copy") ++ 
      audioCodecs(config, audioCount) ++ Seq("-f", "matroska", tempOutPath)
    val result = (command.!)
    if(result == 0) {
      Seq("mv", "-v", tempOutPath, outPath).!
      val result2 = (Seq("rm", "-v", tempInPath).!)
      if (result2 != 0) {println("rm error"); System.exit(result2)}
    }
    if (result != 0) {println("ffmpeg error"); System.exit(result)}
  }
}

// Returns the ammount of audio streams in the given input file
def audioStreams(file: String): Int = {
  val result = os.proc("ffprobe", "-i", file).call()
  result.err.lines.filter(s => s.contains("Stream") && s.contains("Audio")).size
}

// Creates a list of codec options for an input file with n audio streams
def audioCodecs(config: Config, n: Int): Seq[String] = {
  if(config.replaceStreams) {
    val transcodes = for(i<- 0 until n) yield {
      Seq("-c:a:"+i, "libfdk_aac", "-ac", "2", "-vbr", "4")
    }
    transcodes.flatten
  } else {
    val copies = for(i<- 0 until n) yield {
      Seq("-c:a:"+i, "copy")
    }
    val transcodes = for(i<- n until n*2) yield {
      Seq("-c:a:"+i, "libfdk_aac", "-ac", "2", "-vbr", "4")
    }
    (copies ++ transcodes).flatten
  }
}
