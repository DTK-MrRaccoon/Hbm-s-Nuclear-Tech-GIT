local component = require("component")
local gen = component.proxy(component.list("ntm_igen")())

while true do
  local power = gen.getPower()
  local output = gen.getOutput()
  local spin = gen.getSpin()
  local water = gen.getWater()
  local oil = gen.getOil()
  local lube = gen.getLubricant()
  local rtg = gen.hasRTG()
  print(string.format("pwr:%d out:%d spn:%d wtr:%d oil:%d lub:%d rtg:%s", power, output, spin, water, oil, lube, tostring(rtg)))
  os.sleep(1)
end