printf "%b" "Rebuilding..."
mdpress presentation.md
cp -r images/ presentation
printf "%b" "Done\n"
printf "%b" "Watching presentation.md\n"
while :
do
  inotifywait -e modify -q -q presentation.md
  printf "%b" "File modified. "
  printf "%b" "Rebuilding..."
  mdpress presentation.md
  printf "%b" "Done\n"
done
