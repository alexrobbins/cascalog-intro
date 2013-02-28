printf "%b" "Watching presentation.md\n"
while :
do
  printf "%b" "Rebuilding..."
  mdpress presentation.md
  printf "%b" "Done\n"
  inotifywait -e modify -q -q presentation.md
  printf "%b" "File modified. "
done
