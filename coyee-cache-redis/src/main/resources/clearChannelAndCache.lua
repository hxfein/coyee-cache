local namespace=KEYS[1];
local channel=KEYS[2];

--创建栏目key
local function createChannelKey(namespace,channel)
    return namespace .. ":channel:" .. channel;
end
--创建缓存key
local function createKey(namespace,key)
    key = string.gsub(key, ':', '=');
    return namespace .. ":data:" .. key;
end
--删除
local function deleteByKey(key)
    redis.call('DEL', key);
end
--获取set集合
local function members(key)
    local members=redis.call('SMEMBERS', key);
    return members;
end

local channelKey = createChannelKey(namespace,channel);
local keySet=members(channelKey);
for index,key in pairs(keySet) do
    local dataKey=createKey(namespace,key);
    deleteByKey(dataKey);
end
deleteByKey(channelKey);
return 1;