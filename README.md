# audiotranscode
Usage: audiotranscode [OPTION]... [FILE]...  
Convert the audio tracks of the given files to stereo AAC streams, replacing the files with new matroska (.mkv) files.  
By default the transcodes tracks are added as extra tracks, pass -t to replace the originals

Options:  
  -h, --help       Show this message  
  -t               Don't include copies of the original audio streams, only the transcodes  
  -o               Map the transcoded streams ahead of the copies (does nothing when paired with -t)  
  -k               Keep the original files  
  -m               Transcode audio to MP3, rather than AAC
