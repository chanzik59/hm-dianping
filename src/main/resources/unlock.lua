---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by phone.
--- DateTime: 2022/9/20 22:33
---
if (redis.call('get', KEYS[1]) == ARGV[1]) then
    return redis.call('del', KEYS[1])
end
return 0