-- Public-domain original texts; project-authored notes are explicitly labelled.
ALTER TABLE poems ADD COLUMN source_url TEXT;

INSERT INTO poems(poet_id,title,content,context,translation,appreciation,tags,source_url)
SELECT id,'静夜思','床前明月光，疑是地上霜。
举头望明月，低头思故乡。','采用《唐诗三百首》通行本文字；其他版本有“看月光”“望山月”等异文。','项目白话导读：床前洒着月光，恍如地上的白霜。抬头望向明月，低下头，又想起故乡。','项目赏读：月光把眼前的清夜与远方的故乡连在一起。举头、低头的细小动作，使乡愁有了可见的形状。','["思乡","月夜"]'::jsonb,'https://zh.wikisource.org/wiki/%E9%9D%9C%E5%A4%9C%E6%80%9D'
FROM poets WHERE name='李白' AND NOT EXISTS (SELECT 1 FROM poems x WHERE x.poet_id=poets.id AND x.title='静夜思');

INSERT INTO poems(poet_id,title,content,context,translation,appreciation,tags,source_url)
SELECT id,'早发白帝城','朝辞白帝彩云间，千里江陵一日还。
两岸猿声啼不住，轻舟已过万重山。','又题《下江陵》；“啼不住”采用通行异文。','项目白话导读：清晨辞别彩云中的白帝城，一天便能回到千里之外的江陵。两岸猿声还在耳边，小舟已经越过层层群山。','项目赏读：听觉中的猿声还未消散，舟行已远。夸张的路程与轻快的节奏，共同写出身心舒展的瞬间。','["山水","行旅"]'::jsonb,'https://zh.wikisource.org/wiki/%E6%97%A9%E7%99%BC%E7%99%BD%E5%B8%9D%E5%9F%8E'
FROM poets WHERE name='李白' AND NOT EXISTS (SELECT 1 FROM poems x WHERE x.poet_id=poets.id AND x.title='早发白帝城');

INSERT INTO poems(poet_id,title,content,context,translation,appreciation,tags,source_url)
SELECT id,'春望','国破山河在，城春草木深。
感时花溅泪，恨别鸟惊心。
烽火连三月，家书抵万金。
白头搔更短，浑欲不胜簪。','采用通行本；原诗与下方导读分开呈现。','项目白话导读：国都残破，山河依旧；春日城里草木繁茂。感伤时局，见花也落泪；怅恨离别，听鸟也心惊。战火持续，家书格外珍贵。白发越搔越少，几乎插不住发簪。','项目赏读：山河、花鸟并未消失，却在乱世中带来更深的感伤。“家书”让宏大的战乱落到一个人的牵挂上。','["家书","忧思"]'::jsonb,'https://zh.wikisource.org/wiki/%E6%98%A5%E6%9C%9B'
FROM poets WHERE name='杜甫' AND NOT EXISTS (SELECT 1 FROM poems x WHERE x.poet_id=poets.id AND x.title='春望');

INSERT INTO poems(poet_id,title,content,context,translation,appreciation,tags,source_url)
SELECT id,'江南逢李龟年','岐王宅里寻常见，崔九堂前几度闻。
正是江南好风景，落花时节又逢君。','采用“正是”的通行本文字。','项目白话导读：当年常在岐王府见到你，也曾多次在崔九堂前听你唱歌。如今江南风景正好，落花时节，我们又相逢了。','项目赏读：诗中只轻轻说出昔日与今日，把岁月的变化留给读者体会。美景和落花同时出现，使重逢含着难言的惆怅。','["重逢","怀旧"]'::jsonb,'https://zh.wikisource.org/wiki/%E6%B1%9F%E5%8D%97%E9%80%A2%E6%9D%8E%E9%BE%9C%E5%B9%B4'
FROM poets WHERE name='杜甫' AND NOT EXISTS (SELECT 1 FROM poems x WHERE x.poet_id=poets.id AND x.title='江南逢李龟年');

INSERT INTO poems(poet_id,title,content,context,translation,appreciation,tags,source_url)
SELECT id,'题西林壁','横看成岭侧成峰，远近高低各不同。
不识庐山真面目，只缘身在此山中。','“只缘”采用通行异文。','项目白话导读：从正面看是连绵山岭，从侧面看是耸立山峰；远近高低不同，景象也不同。看不全庐山的真实面貌，是因为自己就在山中。','项目赏读：诗从观察角度写起，转向对认识局限的自觉。它既是一幅游山速写，也邀请人暂时走出习惯的立场。','["山水","哲思"]'::jsonb,'https://zh.wikisource.org/wiki/%E9%A1%8C%E8%A5%BF%E6%9E%97%E5%A3%81_(%E8%98%87%E8%BB%BE)'
FROM poets WHERE name='苏轼' AND NOT EXISTS (SELECT 1 FROM poems x WHERE x.poet_id=poets.id AND x.title='题西林壁');

INSERT INTO poems(poet_id,title,content,context,translation,appreciation,tags,source_url)
SELECT id,'饮湖上初晴后雨·其二','水光潋滟晴方好，山色空蒙雨亦奇。
欲把西湖比西子，淡妆浓抹总相宜。','完整收录组诗的第二首。','项目白话导读：晴天湖水闪动，美不胜收；雨中山色朦胧，也别有意趣。若把西湖比作西施，淡妆或浓妆都很合宜。','项目赏读：晴与雨互相映衬，诗人没有执着于一种天气。以人的妆容比湖山气象，让变化中的美变得亲切。','["西湖","山水"]'::jsonb,'https://zh.wikisource.org/wiki/%E9%A3%B2%E6%B9%96%E4%B8%8A%E5%88%9D%E6%99%B4%E5%BE%8C%E9%9B%A8'
FROM poets WHERE name='苏轼' AND NOT EXISTS (SELECT 1 FROM poems x WHERE x.poet_id=poets.id AND x.title='饮湖上初晴后雨·其二');

INSERT INTO poems(poet_id,title,content,context,translation,appreciation,tags,source_url)
SELECT id,'夏日绝句','生当作人杰，死亦为鬼雄。
至今思项羽，不肯过江东。','原题亦作《乌江》；首句“作”、次句“为”采用通行本。','项目白话导读：活着应当成为出众的人，死后也应是英勇的魂。至今还会想起项羽，因为他不肯退回江东。','项目赏读：前两句直接表达价值取向，后两句以历史人物回应。短短二十字，显出与婉约词风不同的刚健一面。','["咏史","志气"]'::jsonb,'https://zh.wikisource.org/wiki/%E7%83%8F%E6%B1%9F_(%E6%9D%8E%E6%B8%85%E7%85%A7)'
FROM poets WHERE name='李清照' AND NOT EXISTS (SELECT 1 FROM poems x WHERE x.poet_id=poets.id AND x.title='夏日绝句');

INSERT INTO poems(poet_id,title,content,context,translation,appreciation,tags,source_url)
SELECT id,'如梦令·常记溪亭日暮','常记溪亭日暮，沉醉不知归路。
兴尽晚回舟，误入藕花深处。
争渡，争渡，惊起一滩鸥鹭。','据《漱玉词》所收篇目，简体转写。','项目白话导读：常想起溪亭日暮，沉醉中忘了归路。尽兴后乘舟回去，却误入荷花深处。忙着划船寻找出路，惊飞了一滩鸥鹭。','项目赏读：“误入”带来意外，“惊起”让画面忽然舒展开来。叠句保留了当时的急切，也让回忆带着鲜活的乐趣。','["回忆","游赏"]'::jsonb,'https://zh.wikisource.org/wiki/%E5%A6%82%E5%A4%A2%E4%BB%A4_(%E6%9D%8E%E6%B8%85%E7%85%A7)/%E5%A6%82%E5%A4%A2%E4%BB%A4_(%E5%B8%B8%E8%A8%98%E6%BA%AA%E4%BA%AD%E6%97%A5%E6%9A%AE)'
FROM poets WHERE name='李清照' AND NOT EXISTS (SELECT 1 FROM poems x WHERE x.poet_id=poets.id AND x.title='如梦令·常记溪亭日暮');

INSERT INTO poems(poet_id,title,content,context,translation,appreciation,tags,source_url)
SELECT id,'相思','红豆生南国，春来发几枝。
愿君多采撷，此物最相思。','“春来”“愿君多采撷”采用通行本文字；原作存在其他异文。','项目白话导读：红豆生在南方，春来又长出多少枝？愿你多采一些，因为它最能寄托思念。','项目赏读：诗没有铺陈离别缘由，而将难以言说的思念托付给一颗红豆。由询问到叮嘱，语气温柔而直接。','["相思","寄赠"]'::jsonb,'https://zh.wikisource.org/wiki/%E7%9B%B8%E6%80%9D'
FROM poets WHERE name='王维' AND NOT EXISTS (SELECT 1 FROM poems x WHERE x.poet_id=poets.id AND x.title='相思');

INSERT INTO poems(poet_id,title,content,context,translation,appreciation,tags,source_url)
SELECT id,'山居秋暝','空山新雨后，天气晚来秋。
明月松间照，清泉石上流。
竹喧归浣女，莲动下渔舟。
随意春芳歇，王孙自可留。','完整五言律诗，简体转写。','项目白话导读：空山刚下过雨，傍晚已有秋意。月光照着松林，清泉流过山石。竹林喧响，是洗衣女子归来；莲叶摇动，是渔舟顺流而下。任春日芳华消歇，这里仍值得停留。','项目赏读：静景之间有人声与舟行，山中生活并不空寂。雨后、月下、泉边的感官细节，构成可居可游的安宁。','["秋日","山水"]'::jsonb,'https://zh.wikisource.org/wiki/%E5%B1%B1%E5%B1%85%E7%A7%8B%E6%9A%9D'
FROM poets WHERE name='王维' AND NOT EXISTS (SELECT 1 FROM poems x WHERE x.poet_id=poets.id AND x.title='山居秋暝');
