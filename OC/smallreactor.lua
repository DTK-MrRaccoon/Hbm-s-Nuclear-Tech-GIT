local component = require("component")
local event = require("event")
local term = require("term")
local computer = require("computer")

-- Find GPU
local gpu
for addr, ctype in component.list() do
  if ctype == "gpu" then
    gpu = component.proxy(addr)
    break
  end
end

-- Find reactor
local reactor
for addr, ctype in component.list() do
  if ctype == "small_reactor" then
    reactor = component.proxy(addr)
    break
  end
end

if not reactor then print("No Small Reactor found!") return end
if not gpu then print("No GPU found!") return end

-- Bind GPU to screen
local screen = component.list("screen")()
if screen then gpu.bind(screen) end

-- Configuration
local AUTO_SHUTDOWN_CORE = 45000
local AUTO_SHUTDOWN_HULL = 90000
local AUTO_START_CORE = 20000
local AUTO_START_HULL = 40000
local UPDATE_INTERVAL = 0.5

-- State
local autoMode = true
local lastFuelPercent = reactor.getFuelPercent()
local fuelHistory = {}
local WHITE = 0xFFFFFF
local RED = 0xFF0000
local YELLOW = 0xFFFF00
local GREEN = 0x00FF00
local BLUE = 0x0088FF

local function round(num)
  return math.floor(num + 0.5)
end

local function setColor(color)
  if gpu then gpu.setForeground(color) end
end

local function resetColor()
  if gpu then gpu.setForeground(WHITE) end
end

local function drawBar(value, max, width, colorType)
  local fill = math.floor((value / max) * width)
  local empty = width - fill
  local bar = string.rep("|", fill) .. string.rep(" ", empty)
  
  if colorType == "red" and fill > width * 0.7 then
    setColor(RED)
  elseif colorType == "yellow" and fill > width * 0.5 then
    setColor(YELLOW)
  else
    setColor(GREEN)
  end
  term.write(bar)
  resetColor()
end

local function formatTime(seconds)
  if seconds < 60 then
    return string.format("%ds", seconds)
  elseif seconds < 3600 then
    return string.format("%dm %ds", math.floor(seconds/60), seconds % 60)
  else
    local h = math.floor(seconds/3600)
    local m = math.floor((seconds%3600)/60)
    return string.format("%dh %dm", h, m)
  end
end

local function updateDisplay()
  local info = reactor.getInfo()
  local coreHeat = info.coreHeat
  local hullHeat = info.hullHeat
  local water = info.water
  local coolant = info.coolant
  local steam = info.steam
  local rods = info.rods
  local fuel = info.fuelPercent

  gpu.fill(1, 1, 80, 25, " ")
  term.setCursor(1,1)
  term.write("SML REACTOR CONTROL\n")
  term.write(string.format("Status: %s\n", rods >= 100 and "ONLINE" or (rods > 0 and "STARTING" or "OFFLINE")))
  term.write(string.format("Auto-shutdown: %s\n", autoMode and "ENABLED" or "DISABLED"))
  term.write("\n")

  term.write("Core Heat: ")
  drawBar(coreHeat, 50000, 30, "red")
  term.write(string.format(" %d/%d\n", coreHeat, 50000))

  term.write("Hull Heat: ")
  drawBar(hullHeat, 100000, 30, "red")
  term.write(string.format(" %d/%d\n", hullHeat, 100000))

  term.write("Water:     ")
  drawBar(water, 32000, 30, "blue")
  term.write(string.format(" %d/32000 mB\n", water))

  term.write("Coolant:   ")
  drawBar(coolant, 16000, 30, "blue")
  term.write(string.format(" %d/16000 mB\n", coolant))

  term.write("Steam:     ")
  drawBar(steam, 128000, 30, "yellow")
  term.write(string.format(" %d/128000 mB\n", steam))

  term.write("Fuel:      ")
  drawBar(fuel, 100, 30, "green")
  term.write(string.format(" %d%%\n", fuel))

  term.write("\n")
  term.write("Controls: [O] Start  [P] Stop  [A] Toggle Auto  [Q] Quit\n")

  if #fuelHistory > 1 then
    local depletionRate = (fuelHistory[1] - fuelHistory[#fuelHistory]) / (#fuelHistory - 1)
    if depletionRate > 0 then
      local remainingTicks = (fuel / depletionRate) * UPDATE_INTERVAL * 20
      term.write(string.format("\nEst. time remaining: %s\n", formatTime(remainingTicks)))
    end
  end
end

local function checkAutoShutdown()
  if not autoMode then return end
  local info = reactor.getInfo()
  if info.coreHeat >= AUTO_SHUTDOWN_CORE or info.hullHeat >= AUTO_SHUTDOWN_HULL then
    if info.rods >= 100 then
      reactor.setRodsActive(false)
      term.setCursor(1, 15)
      term.write("!!! AUTO-SHUTDOWN TRIGGERED !!!")
    end
  elseif info.coreHeat <= AUTO_START_CORE and info.hullHeat <= AUTO_START_HULL then
    if info.rods == 0 then
      reactor.setRodsActive(true)
    end
  end
end

local running = true
while running do
  updateDisplay()
  local e = {event.pull(UPDATE_INTERVAL)}
  if e[1] == "key_down" then
    local key = e[4]
    if key == 24 then reactor.setRodsActive(true)      -- O
    elseif key == 25 then reactor.setRodsActive(false) -- P
    elseif key == 30 then autoMode = not autoMode      -- A
    elseif key == 16 then running = false              -- Q
    end
  end

  local currentFuel = reactor.getFuelPercent()
  if currentFuel ~= lastFuelPercent then
    table.insert(fuelHistory, currentFuel)
    if #fuelHistory > 60 then table.remove(fuelHistory, 1) end
    lastFuelPercent = currentFuel
  end

  checkAutoShutdown()
end

gpu.fill(1, 1, 80, 25, " ")
term.setCursor(1,1)
print("Reactor control terminated.")