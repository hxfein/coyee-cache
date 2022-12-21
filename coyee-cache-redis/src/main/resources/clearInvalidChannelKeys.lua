local namespace=KEYS[1];
local channel=KEYS[2];

--获取set集合
local function members(key)
    local members=redis.call('SMEMBERS', key);
    return members;
end
--判断key是否存在
local function exists(key)
    return redis.call('EXISTS',key)
end
--从set移除key
local function removeFromSet(channelKey,dataKey)
    return redis.call('SREM',channelKey,dataKey)
end
--创建栏目key
local function createChannelKey(namespace,channel)
    return namespace .. ":channel:" .. channel;
end
--创建缓存key
local function createKey(namespace,key)
    key = string.gsub(key, ':', '=');
    return namespace .. ":data:" .. key;
end
local removeCount=0;
local channelKey=createChannelKey(namespace,channel);
local keySet=members(channelKey);
for index,key in pairs(keySet) do
    local dataKey=createKey(namespace,key);
    if(exists(dataKey)==0) then
        removeFromSet(channelKey,key);
        removeCount=removeCount+1;
    end
end
return removeCount;