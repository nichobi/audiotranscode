#!/bin/sh
exec amm "$0" "$@"
!#

import scala.sys.process._
import os.Path

@main
def main(args: String*): Unit = {
  val (options, fileStrings) = args.partition(_.startsWith("-"))
  val config = parseOptions(options)
  val files = fileStrings.map(x => Path(x, base=os.pwd))
  run(config, files)
}

case class Config(replace: Boolean = false)

def parseOptions(options: Seq[String]): Config = {
  var config = Config()
  options foreach {
    case "-h" => {
      println("""Usage: audiotranscode [OPTION]... [FILE]...
      Convert the audio tracks of the given files to stereo AAC streams, replacing the files with new matroska (.mkv) files.
      By default the transcodes tracks are added as extra tracks, pass -r to replace the originals""".stripMargin)
      sys.exit()}
    case "-r" => config=config.copy(replace=true)
    case _ => println("Invalid args")
      sys.exit
  }
  config
}

def run(config: Config, files: Seq[Path]) {
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
      audioCodecs(audioCount) ++ Seq("-f", "matroska", tempOutPath)
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
  val out = new StringBuilder
  val err = new StringBuilder
  val logger = ProcessLogger(
    (o: String) => out.append(o + '\n'),
    (e: String) => err.append(e + '\n'))
  Seq("ffprobe", "-i", file).!(logger)
  err.split('\n').filter(s => s.contains("Stream") && s.contains("Audio")).size
}

// Creates a list of codec options for an input file with n audio streams
def audioCodecs(n: Int): Seq[String] = {
  val copies = for(i<- 0 until n) yield {
    Seq("-c:a:"+i, "copy")
  }
  val transcodes = for(i<- n until n*2) yield {
    Seq("-c:a:"+i, "libfdk_aac", "-ac", "2", "-vbr", "4")
  }
  (copies ++ transcodes).flatten
}
