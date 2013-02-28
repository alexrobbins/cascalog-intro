printf "%b" "Watching presentation.md\n"
while :
do
  inotifywait -e modify -q -q presentation.md
  printf "%b" "File modified. Rebuilding..."
  mdpress presentation.md
  printf "%b" "Done\n"
done
