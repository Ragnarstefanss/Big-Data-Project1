s = set()
with open("movies_org.txt", encoding="UTF-8") as f:
    for line in f.readlines():
        s.add(line.rstrip())

with open("movies.txt", "w", encoding="UTF-8") as f:
    f.write("\n".join(line for line in sorted(s)))
