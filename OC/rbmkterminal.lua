local component = require("component")
local term = component.rbmk_terminal

local S = "\194\167"
term.enableOCMode(true)
term.clearScreen()

term.write(S.."f"..S.."lFULL FORMATTING REFERENCE")

-- Organized in pairs to fit the 27-char width
term.write(S.."0#0 BLACK     "..S.."8#8 D-GRAY")
term.write(S.."1#1 D-BLUE    "..S.."9#9 BLUE")
term.write(S.."2#2 D-GREEN   "..S.."a#a GREEN")
term.write(S.."3#3 D-AQUA    "..S.."b#b AQUA")
term.write(S.."4#4 D-RED     "..S.."c#c RED")
term.write(S.."5#5 D-PURPLE  "..S.."d#d PINK")
term.write(S.."6#6 GOLD      "..S.."e#e YELLOW")
term.write(S.."7#7 GRAY      "..S.."f#f WHITE")

term.write(S.."8---------------------------")

term.write(S.."f"..S.."k#k "..S.."r"..S.."fOBFUSCATED (MAGIC)")
term.write(S.."f"..S.."l#l BOLD TEXT")
term.write(S.."f"..S.."m#m STRIKETHROUGH")
term.write(S.."f"..S.."n#n UNDERLINE")
term.write(S.."f"..S.."o#o ITALIC")