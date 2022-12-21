local namespace=KEYS[1];
local channel=KEYS[2];

--创建栏目key
local function createChannelKey(namespace,channel)
    return namespace .. "/channel/" .. channel;
end
--创建关系key
local function createLinkKey(namespace,channel)
    return namespace .. "/links/" .. channel;
end
--创建缓存key
local function createKey(namespace,key)
    key = string.gsub(key, '/', '=');
    return namespace .. "/data/" .. key;
end
--删除
local function deleteByKey(key)
    redis.call('del', key);
end
--获取set集合
local function members(key)
    local members=redis.call('SMEMBERS', key);
    return members;
end

local flag='';
local linkKey = createLinkKey(namespace,channel);
flag=flag..linkKey..',';
local linkChannels=members(linkKey);
flag=flag..'['..table.getn(linkChannels)..']';
flag=flag..'['..type(linkChannels)..']';
flag=flag..cjson.encode(linkChannels)..'====';
for k1,v1 in pairs(linkChannels) do
--     flag=flag..'<br/>'..i1..'='..linkChannels[i1]..'='..v1;
	local linkChannel=v1;
	flag=flag..linkChannel..',';
    local channelKey = createChannelKey(namespace,linkChannel);
    flag=flag..channelKey..',';
    local linkChannelKey = createLinkKey(namespace,linkChannel);
    flag=flag..linkChannelKey..',';
--     flag=flag..channelKey..'==='..linkChannelKey;
    local keySet=members(channelKey);
    for k2,v2 in pairs(keySet) do
        local key=createKey(namespace,v2);
        deleteByKey(key);
    end
    deleteByKey(channelKey);
    deleteByKey(linkChannelKey);
end
return '"'..flag..'"';