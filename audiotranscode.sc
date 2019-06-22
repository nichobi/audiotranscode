#!/bin/sh
exec amm --silent "$0" "$@"
!#

import scala.sys.process._
import os.{Path, Shellable, proc}
import $file.util

@main
def main(args: String*): Unit = {
  val (options, fileStrings) = args.partition(_.startsWith("-"))
  implicit val config = parseOptions(options)
  val files = fileStrings.map(x => Path(x, base=os.pwd))
  run(files)
}

case class Config(
  replaceStreams: Boolean = false,
  transcodesFirst: Boolean = false,
  replaceFiles: Boolean = true,
  mp3: Boolean = true)

def parseOptions(options: Seq[String]): Config = {
  var config = Config()
  options foreach {
    case "-h"|"--help" => {
      println("""Usage: audiotranscode [OPTION]... [FILE]...
      |Convert the audio tracks of the given files to stereo AAC streams, replacing the files with new matroska (.mkv) files.
      |By default the transcodes tracks are added as extra tracks, pass -t to replace the originals
      |
      |Options:
      |  -h, --help       Show this message
      |  -t               Don't include copies of the original audio streams, only the transcodes
      |  -o               Map the transcoded streams ahead of the copies (does nothing when paired with -t)
      |  -k               Keep the original files
      |  -m               Transcode audio to MP3, rather than AAC""".stripMargin)
      sys.exit()}
    case "-t" => config = config.copy(replaceStreams = true)
    case "-o" => config = config.copy(transcodesFirst = true)
    case "-k" => config = config.copy(replaceFiles = false)
    case _ => println("Invalid args")
      sys.exit
  }
  config
}

def createFfmpegCommand(input: Path, output: Path)(implicit config: Config): proc  = {
  val initialArgs = Seq[Shellable]("ffmpeg", "-y", "-nostats", "-hide_banner", 
		"-fflags", "+genpts", "-i", input.toString, "-map", "0", "-map", "0:a",
		"-flags", "+global_header", "-codec", "copy")
  val finalArgs = Seq[Shellable]("-f", "matroska", output.toString)
  val audioCount = audioStreams(input)
  proc(initialArgs ++ audioCodecs(audioCount) ++ finalArgs)
}

def run(files: Seq[Path])(implicit config: Config) {
  for (file <- files) {
    if (config.replaceFiles) {
      val parent = file / os.up
      val base = file.baseName
      val tempInPath = parent / (base + ".original-temp")
      val outPath = parent / (base + ".mkv")
      val tempOutPath = parent / (base + ".new-temp")
      util.move(file, tempInPath)
      val command = createFfmpegCommand(tempInPath, tempOutPath)
      println(s"FFmpeg command:\n  ${commandToString(command)}")
      val result = command.call()
      println("ffmpeg complete!")
      val streamMapping = result.err.lines.span(!_.contains("Stream mapping:"))
        ._2.takeWhile(_.contains("Stream"))
      streamMapping.foreach(println)
      if(result.exitCode == 0) {
        util.move(tempOutPath, outPath)
        os.remove(tempInPath)
      }
    } else {
      val parent = file / os.up
      val base = file.baseName
      val outPath = parent / (base + "-transcoded.mkv")
      val tempOutPath = parent / (base + ".transcoded-temp")
      val command = createFfmpegCommand(file, tempOutPath)
      println(s"FFmpeg command:\n  ${commandToString(command)}")
      val result = command.call()
      println("FFmpeg complete!")
      val streamMapping = result.err.lines.span(!_.contains("Stream mapping:"))
        ._2.takeWhile(_.contains("Stream"))
      streamMapping.foreach(println)
      if(result.exitCode == 0) {
        util.move(tempOutPath, outPath)
      }
    }
  }
}

// Returns the ammount of audio streams in the given input file
def audioStreams(file: Path): Int = {
  val result = proc("ffprobe", "-i", file).call()
  result.err.lines.filter(s => s.contains("Stream") && s.contains("Audio")).size
}

// Creates a list of codec options for an input file with n audio streams
def audioCodecs(n: Int)(implicit config: Config): Seq[Shellable] = {
  if(config.replaceStreams) {
    val transcodes = for(i<- 0 until n) yield codecString(i)
    transcodes.flatten
  } else {
    val first = for(i<- 0 until n) yield {
      if(config.transcodesFirst) codecString(i)
      else Seq[Shellable]("-c:a:"+i, "copy")
    }
    val second = for(i<- n until n*2) yield {
      if(config.transcodesFirst) Seq[Shellable]("-c:a:"+i, "copy")
      else codecString(i)
    }
    (first ++ second).flatten
  }
}

def codecString(n: Int)(implicit config: Config) = {
  if(config.mp3) {
    Seq[Shellable]("-c:a:" + n.toString, "libmp3lame", "-ac", "2", "-b:a", "320k")
  } else {
    Seq[Shellable]("-c:a:" + n.toString, "libfdk_aac", "-ac", "2", "-vbr", "4")
  }
}

def commandToString(command: os.proc) = {
  command.command(0).value.mkString(" ")
}
