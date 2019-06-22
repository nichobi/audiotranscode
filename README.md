# audiotranscode
audiotranscode is a script to easily transcode the audio tracks of video files, to stereo AAC or MP3 tracks, which in some cases play more nicely with Chromecasts, etc.  
It is written in Scala and depends on [Ammonite](https://github.com/lihaoyi/Ammonite), a Scala scripting tool; and [FFmpeg](https://ffmpeg.org/), an incredibly versatile and high quality video transcoder and muxer.
```
Usage: audiotranscode [OPTION]... [FILE]...  
Convert the audio tracks of the given files to stereo AAC streams, replacing 
the files with new matroska (.mkv) files.  

Options:  
  -h, --help      Show this message  
  -t              Don't include copies of the original audio streams
  -o              Map the transcoded streams ahead of the copies 
                  (does nothing when paired with -t)  
  -k              Keep the original files  
  -m              Transcode audio to MP3, rather than AAC
  ```

