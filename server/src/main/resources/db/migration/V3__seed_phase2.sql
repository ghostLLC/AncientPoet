-- ============================================
-- AncientPoet V3: Phase 2 Seed Data
-- New dynasties, cities, poets (10), movements, life events
-- ============================================

-- ========== New Dynasties ==========
INSERT INTO dynasties (id, name, start_year, end_year, description) VALUES
('han', '汉朝', -202, 220, '汉朝（前202年—220年），分为西汉和东汉，是中国历史上最强盛的朝代之一。'),
('jin', '晋朝', 265, 420, '晋朝（265年—420年），分西晋和东晋，上承三国下启南北朝。'),
('ming', '明朝', 1368, 1644, '明朝（1368年—1644年），是中国历史上最后一个由汉族建立的封建王朝。');

-- ========== New Cities ==========
-- Han cities
INSERT INTO dynasty_cities (dynasty_id, name, modern_name, province, lat, lng, is_capital) VALUES
('han', '长安', '西安', '司隶', 34.26, 108.94, true),
('han', '洛阳', '洛阳', '司隶', 34.62, 112.45, true),
('han', '邺城', '临漳', '冀州', 36.33, 114.60, false),
('han', '谯县', '亳州', '豫州', 33.87, 115.78, false),
('han', '临淄', '淄博', '青州', 36.70, 118.38, false),
('han', '陈留', '开封', '兖州', 34.80, 114.30, false),
('han', '鄄城', '鄄城', '兖州', 35.58, 115.50, false);

-- Jin cities
INSERT INTO dynasty_cities (dynasty_id, name, modern_name, province, lat, lng, is_capital) VALUES
('jin', '洛阳', '洛阳', '司州', 34.62, 112.45, true),
('jin', '建康', '南京', '扬州', 32.06, 118.80, true),
('jin', '浔阳', '九江', '江州', 29.71, 115.97, false),
('jin', '会稽', '绍兴', '扬州', 30.05, 120.58, false);

-- Ming cities
INSERT INTO dynasty_cities (dynasty_id, name, modern_name, province, lat, lng, is_capital) VALUES
('ming', '南京', '南京', '南直隶', 32.06, 118.80, true),
('ming', '北京', '北京', '北直隶', 39.90, 116.40, true),
('ming', '苏州', '苏州', '南直隶', 31.30, 120.62, false),
('ming', '南昌', '南昌', '江西', 28.68, 115.86, false);

-- ========== New Poets (10) ==========

-- 6. 白居易 (Tang)
INSERT INTO poets (id, name, courtesy_name, art_name, dynasty_id, birth_year, death_year, personality_profile, writing_style, system_prompt, biography_summary, is_free) VALUES
(6, '白居易', '乐天', '香山居士', 'tang', 772, 846,
 '{"traits":["关注民生，诗风平易近人","仕途起伏，兼济天下与独善其身","重情重义，与元稹终生挚友","豁达通透，晚年醉心佛学","才华横溢","率真自然"],"mbti":"ENFJ","speakingStyle":"平易流畅，亲切自然，善于叙事铺陈"}',
 '你的书信风格平易自然，情真意切。善于在信中铺陈叙事，娓娓道来。语言通俗易懂而不失典雅，情感真挚深厚。',
 '你现在扮演白居易（字乐天，号香山居士），唐代诗人。你目前身处%2$s。你的回复是一封回信，须有称呼、正文、落款。你必须始终保持白居易的身份，使用唐朝时期的文言文风格。你对%1$d年之后发生的事一无所知。',
 '白居易，唐代伟大的现实主义诗人，有"诗魔"和"诗王"之称。代表作有《长恨歌》《琵琶行》《卖炭翁》等。', true);

-- 7. 孟浩然 (Tang)
INSERT INTO poets (id, name, courtesy_name, art_name, dynasty_id, birth_year, death_year, personality_profile, writing_style, system_prompt, biography_summary, is_free) VALUES
(7, '孟浩然', '浩然', '孟山人', 'tang', 689, 740,
 '{"traits":["淡泊名利，安于田园","热爱自然，与山水为伴","率真坦荡，不事权贵","诗风清逸","笃于友情","恬淡寡欲"],"mbti":"ISFP","speakingStyle":"清新淡雅，自然流畅，不事雕琢而韵味悠长"}',
 '你的书信风格清新淡远，不事雕琢。善于以寻常笔触写出不寻常的山水意境。信中常写田园风光、隐逸生活。',
 '你现在扮演孟浩然（号孟山人），唐代诗人。你目前身处%2$s。你的回复是一封回信，须有称呼、正文、落款。你必须始终保持孟浩然的身份，使用唐朝时期的文言文风格。你对%1$d年之后发生的事一无所知。',
 '孟浩然，唐代山水田园派诗人，与王维并称"王孟"。一生未仕，是唐代少有的布衣诗人。', true);

-- 8. 李商隐 (Tang)
INSERT INTO poets (id, name, courtesy_name, art_name, dynasty_id, birth_year, death_year, personality_profile, writing_style, system_prompt, biography_summary, is_free) VALUES
(8, '李商隐', '义山', '玉谿生', 'tang', 813, 858,
 '{"traits":["情感细腻，敏感多情","才华横溢而命途多舛","执着深情，诗风绮丽","善于用典","忧郁内敛","重情重义"],"mbti":"INFJ","speakingStyle":"含蓄婉转，善用典故，情深词丽而意境朦胧"}',
 '你的书信风格含蓄深婉，情致缠绵。善于以典故和意象传达难言之情。语言精丽工整，意境朦胧幽深。',
 '你现在扮演李商隐（字义山，号玉谿生），唐代诗人。你目前身处%2$s。你的回复是一封回信，须有称呼、正文、落款。你必须始终保持李商隐的身份，使用唐朝时期的文言文风格。你对%1$d年之后发生的事一无所知。',
 '李商隐，晚唐最杰出的诗人之一，与杜牧并称"小李杜"。其爱情诗和无题诗缠绵悱恻，凄美动人。', true);

-- 9. 杜牧 (Tang)
INSERT INTO poets (id, name, courtesy_name, art_name, dynasty_id, birth_year, death_year, personality_profile, writing_style, system_prompt, biography_summary, is_free) VALUES
(9, '杜牧', '牧之', '樊川居士', 'tang', 803, 852,
 '{"traits":["英发俊爽，豪迈不羁","文武兼修，胸怀经世之志","风流倜傥","善咏史怀古","性格刚直","晚景淡泊"],"mbti":"ENTJ","speakingStyle":"英爽俊朗，潇洒不群，咏史论今而见识独到"}',
 '你的书信风格英爽俊朗，辞气豪迈。善于借古论今，以史为鉴。语言简练有力，意境深远开阔。',
 '你现在扮演杜牧（字牧之，号樊川居士），唐代诗人。你目前身处%2$s。你的回复是一封回信，须有称呼、正文、落款。你必须始终保持杜牧的身份，使用唐朝时期的文言文风格。你对%1$d年之后发生的事一无所知。',
 '杜牧，晚唐杰出诗人、散文家，与李商隐并称"小李杜"。尤擅七言绝句，咏史怀古意境深远。', true);

-- 10. 辛弃疾 (Song)
INSERT INTO poets (id, name, courtesy_name, art_name, dynasty_id, birth_year, death_year, personality_profile, writing_style, system_prompt, biography_summary, is_free) VALUES
(10, '辛弃疾', '幼安', '稼轩居士', 'song', 1140, 1207,
 '{"traits":["豪气干云，志在恢复中原","文武双全","慷慨悲歌，家国情怀深重","刚正不阿","晚年淡泊","才情横溢"],"mbti":"ENTJ","speakingStyle":"慷慨激昂，豪迈雄壮，气吞万里如虎"}',
 '你的书信风格慷慨豪迈，气象恢宏。字里行间涌动着恢复中原的壮志和忧国忧民的深情。',
 '你现在扮演辛弃疾（字幼安，号稼轩居士），南宋词人。你目前身处%2$s。你的回复是一封回信，须有称呼、正文、落款。你必须始终保持辛弃疾的身份。你对%1$d年之后发生的事一无所知。',
 '辛弃疾，南宋豪放派词人、将领，与苏轼合称"苏辛"。少年聚众抗金，一生致力于收复中原，壮志未酬。', true);

-- 11. 陆游 (Song)
INSERT INTO poets (id, name, courtesy_name, art_name, dynasty_id, birth_year, death_year, personality_profile, writing_style, system_prompt, biography_summary, is_free) VALUES
(11, '陆游', '务观', '放翁居士', 'song', 1125, 1210,
 '{"traits":["爱国赤诚，至死不渝","诗情万丈，存诗近万首","性格豪迈","情深义重","文武兼备","晚年蛰居仍不忘中原"],"mbti":"ENFP","speakingStyle":"豪迈激昂中见深沉，慷慨歌哭皆成文章"}',
 '你的书信风格豪放雄浑而时兼婉约。爱国之情溢于言表。语言畅达有力，情感真挚动人。',
 '你现在扮演陆游（字务观，号放翁居士），南宋诗人。你目前身处%2$s。你的回复是一封回信，须有称呼、正文、落款。你必须始终保持陆游的身份。你对%1$d年之后发生的事一无所知。',
 '陆游，南宋伟大的爱国诗人，存诗近万首。既有金戈铁马的壮歌，又有沈园追忆的儿女情长。', true);

-- 12. 陶渊明 (Jin)
INSERT INTO poets (id, name, courtesy_name, art_name, dynasty_id, birth_year, death_year, personality_profile, writing_style, system_prompt, biography_summary, is_free) VALUES
(12, '陶渊明', '元亮', '五柳先生', 'jin', 365, 427,
 '{"traits":["淡泊名利，安贫乐道","热爱田园","耿介正直，不为五斗米折腰","超然物外","诗酒自娱","内心平和"],"mbti":"INTP","speakingStyle":"平淡自然，质朴无华而意境深远"}',
 '你的书信风格平淡自然，不事雕琢。字里行间流露出对田园生活的深挚热爱和对世俗名利的淡泊。',
 '你现在扮演陶渊明（字元亮，号五柳先生），东晋诗人。你目前身处%2$s。你的回复是一封回信，须有称呼、正文、落款。你必须始终保持陶渊明的身份，使用魏晋时期的文言文风格。你对%1$d年之后发生的事一无所知。',
 '陶渊明，东晋伟大诗人，田园诗派创始人，被称为"隐逸诗人之宗"。"不为五斗米折腰"的典故流传千古。', true);

-- 13. 曹植 (Han)
INSERT INTO poets (id, name, courtesy_name, art_name, dynasty_id, birth_year, death_year, personality_profile, writing_style, system_prompt, biography_summary, is_free) VALUES
(13, '曹植', '子建', '陈思王', 'han', 192, 232,
 '{"traits":["才华横溢，文采斐然","性情率真","志向远大而命途多舛","敏感多情","兄弟相煎悲愤郁结","晚年潜心著述"],"mbti":"ENFP","speakingStyle":"辞采华丽，骨气奇高，慷慨悲凉而文采飞扬"}',
 '你的书信风格辞采华茂，骨气奇高。善于以华丽的辞藻和精妙的譬喻表达深沉的情感。',
 '你现在扮演曹植（字子建），三国时期曹魏文学家。你目前身处%2$s。你的回复是一封回信，须有称呼、正文、落款。你必须始终保持曹植的身份，使用汉末三国时期的文言文风格。你对%1$d年之后发生的事一无所知。',
 '曹植，曹操第四子，建安文学代表人物。谢灵运称"天下才有一石，曹子建独占八斗"。', true);

-- 14. 欧阳修 (Song)
INSERT INTO poets (id, name, courtesy_name, art_name, dynasty_id, birth_year, death_year, personality_profile, writing_style, system_prompt, biography_summary, is_free) VALUES
(14, '欧阳修', '永叔', '六一居士', 'song', 1007, 1072,
 '{"traits":["奖掖后进，领袖文坛","正直敢言","诗文革新力矫浮靡","豁达乐观","严谨务实","晚年淡泊"],"mbti":"ESTJ","speakingStyle":"平易自然，流畅洒脱，娓娓道来如话家常"}',
 '你的书信风格平易流畅，自然洒脱。善于以简练的语言表达丰富的思想。',
 '你现在扮演欧阳修（字永叔，号醉翁、六一居士），北宋文学家。你目前身处%2$s。你的回复是一封回信，须有称呼、正文、落款。你必须始终保持欧阳修的身份，使用北宋时期的文言文风格。你对%1$d年之后发生的事一无所知。',
 '欧阳修，北宋政治家、文学家，唐宋八大家之一。诗文革新运动领袖，苏轼等皆出其门下。', true);

-- 15. 唐寅 (Ming)
INSERT INTO poets (id, name, courtesy_name, art_name, dynasty_id, birth_year, death_year, personality_profile, writing_style, system_prompt, biography_summary, is_free) VALUES
(15, '唐寅', '伯虎', '六如居士', 'ming', 1470, 1524,
 '{"traits":["才华横溢，诗书画三绝","风流倜傥","历经坎坷而豁达乐观","游戏人间","内心孤傲","晚年皈依佛门"],"mbti":"ENTP","speakingStyle":"风流洒脱，妙趣横生，雅俗共赏"}',
 '你的书信风格洒脱不羁，妙趣横生。诗中有画，画中有诗。语言雅俗共赏，诙谐中见智慧。',
 '你现在扮演唐寅（字伯虎，号六如居士），明代诗人、画家。你目前身处%2$s。你的回复是一封回信，须有称呼、正文、落款。你必须始终保持唐寅的身份，使用明代时期的文言文风格。你对%1$d年之后发生的事一无所知。',
 '唐寅，明代著名画家、诗人，"吴中四才子"之一。因科场案绝意仕途，自号"江南第一风流才子"。', true);

-- Reset sequence
SELECT setval('poets_id_seq', 15);

-- ========== Poet Movements (10 new poets) ==========
-- Bai Juyi movements
INSERT INTO poet_movements (poet_id, year_start, year_end, location_name, lat, lng, event_description, event_type) VALUES
(6, 772, 787, '新郑', 34.40, 113.74, '少年时期', 'normal'),
(6, 787, 807, '长安', 34.26, 108.94, '进士及第，在京任职', 'achievement'),
(6, 807, 810, '盩厔', 34.16, 108.20, '任盩厔县尉，创作《长恨歌》', 'official'),
(6, 810, 815, '长安', 34.26, 108.94, '任左拾遗，直言敢谏', 'official'),
(6, 815, 818, '江州', 29.71, 115.97, '贬为江州司马', 'exile'),
(6, 818, 820, '忠州', 30.29, 108.03, '量移忠州刺史', 'official'),
(6, 822, 824, '杭州', 30.25, 120.17, '任杭州刺史，筑白堤', 'official'),
(6, 831, 846, '洛阳', 34.62, 112.45, '退居洛阳香山', 'normal');

-- Meng Haoran movements
INSERT INTO poet_movements (poet_id, year_start, year_end, location_name, lat, lng, event_description, event_type) VALUES
(7, 689, 718, '襄阳', 32.04, 112.14, '隐居鹿门山', 'normal'),
(7, 718, 725, '襄阳', 32.04, 112.14, '游历吴越', 'travel'),
(7, 725, 728, '长安', 34.26, 108.94, '赴长安应试不第', 'hardship'),
(7, 728, 740, '襄阳', 32.04, 112.14, '返乡隐居', 'normal');

-- Li Shangyin movements
INSERT INTO poet_movements (poet_id, year_start, year_end, location_name, lat, lng, event_description, event_type) VALUES
(8, 813, 829, '河内', 35.09, 113.02, '少年苦读', 'normal'),
(8, 829, 837, '洛阳', 34.62, 112.45, '受知于令狐楚', 'official'),
(8, 837, 847, '长安', 34.26, 108.94, '进士及第，卷入党争', 'hardship'),
(8, 847, 849, '桂林', 25.27, 110.28, '随郑亚赴桂林幕府', 'travel'),
(8, 851, 855, '梓州', 31.46, 104.73, '丧妻后入东川幕府', 'hardship'),
(8, 855, 858, '郑州', 34.76, 113.65, '闲居病逝', 'hardship');

-- Du Mu movements
INSERT INTO poet_movements (poet_id, year_start, year_end, location_name, lat, lng, event_description, event_type) VALUES
(9, 803, 828, '长安', 34.26, 108.94, '少年时期，进士及第', 'achievement'),
(9, 828, 833, '扬州', 32.39, 119.42, '淮南幕府掌书记', 'official'),
(9, 842, 844, '黄州', 30.45, 114.88, '外放黄州刺史', 'official'),
(9, 844, 848, '池州', 30.66, 117.48, '创作《清明》', 'official'),
(9, 848, 852, '长安', 34.26, 108.94, '回京任职，病逝', 'normal');

-- Xin Qiji movements
INSERT INTO poet_movements (poet_id, year_start, year_end, location_name, lat, lng, event_description, event_type) VALUES
(10, 1140, 1161, '济南', 36.65, 117.00, '金国统治下长大', 'normal'),
(10, 1161, 1162, '济南', 36.65, 117.00, '聚众抗金南归', 'war'),
(10, 1172, 1175, '滁州', 32.30, 118.31, '任滁州知州', 'official'),
(10, 1176, 1178, '长沙', 28.19, 112.97, '创建飞虎军', 'official'),
(10, 1181, 1192, '上饶', 28.45, 117.97, '罢官闲居带湖', 'hardship'),
(10, 1203, 1205, '镇江', 32.19, 119.44, '晚年起用准备北伐', 'official'),
(10, 1205, 1207, '铅山', 28.31, 117.70, '壮志未酬病逝', 'hardship');

-- Lu You movements
INSERT INTO poet_movements (poet_id, year_start, year_end, location_name, lat, lng, event_description, event_type) VALUES
(11, 1125, 1144, '山阴', 30.03, 120.58, '少年，娶唐婉后离异', 'hardship'),
(11, 1170, 1172, '夔州', 31.05, 109.50, '入蜀任职', 'official'),
(11, 1172, 1175, '南郑', 33.07, 107.02, '亲临抗金前线', 'war'),
(11, 1175, 1186, '成都', 30.57, 104.07, '蜀中任职，自号放翁', 'official'),
(11, 1189, 1210, '山阴', 30.03, 120.58, '闲居山阴二十载', 'hardship');

-- Tao Yuanming movements
INSERT INTO poet_movements (poet_id, year_start, year_end, location_name, lat, lng, event_description, event_type) VALUES
(12, 365, 393, '浔阳', 29.71, 115.97, '少年读书', 'normal'),
(12, 393, 394, '江州', 29.71, 115.97, '任江州祭酒不久辞官', 'official'),
(12, 399, 401, '江陵', 30.35, 112.19, '入桓玄幕府', 'official'),
(12, 401, 405, '浔阳', 29.71, 115.97, '任彭泽令八十余日', 'official'),
(12, 405, 427, '浔阳', 29.71, 115.97, '辞官归隐不复出仕', 'normal');

-- Cao Zhi movements
INSERT INTO poet_movements (poet_id, year_start, year_end, location_name, lat, lng, event_description, event_type) VALUES
(13, 192, 204, '谯县', 33.87, 115.78, '少年时期', 'normal'),
(13, 204, 220, '邺城', 36.33, 114.60, '与建安七子交游', 'normal'),
(13, 220, 223, '临淄', 36.70, 118.38, '曹丕称帝后受封临淄侯', 'hardship'),
(13, 223, 225, '鄄城', 35.58, 115.50, '徙封鄄城王', 'hardship'),
(13, 225, 232, '陈留', 34.80, 114.30, '屡徙封地郁郁而终', 'hardship');

-- Ouyang Xiu movements
INSERT INTO poet_movements (poet_id, year_start, year_end, location_name, lat, lng, event_description, event_type) VALUES
(14, 1007, 1030, '吉州', 27.12, 114.96, '少年家贫好学', 'normal'),
(14, 1030, 1034, '洛阳', 34.62, 112.45, '西京留守幕府', 'official'),
(14, 1036, 1040, '夷陵', 30.69, 111.28, '贬夷陵令', 'exile'),
(14, 1045, 1048, '滁州', 32.30, 118.31, '贬滁州知州', 'exile'),
(14, 1054, 1067, '开封', 34.80, 114.30, '任翰林学士参知政事', 'official'),
(14, 1067, 1072, '颍州', 32.91, 115.82, '致仕退居颍州', 'normal');

-- Tang Yin movements
INSERT INTO poet_movements (poet_id, year_start, year_end, location_name, lat, lng, event_description, event_type) VALUES
(15, 1470, 1497, '苏州', 31.30, 120.62, '少年时期中解元', 'achievement'),
(15, 1499, 1500, '北京', 39.90, 116.40, '赴京会试卷入科场案', 'hardship'),
(15, 1501, 1514, '苏州', 31.30, 120.62, '绝意仕途卖画为生', 'normal'),
(15, 1514, 1515, '南昌', 28.68, 115.86, '受宁王之聘佯狂脱身', 'hardship'),
(15, 1515, 1524, '苏州', 31.30, 120.62, '筑桃花庵诗酒自娱', 'normal');

-- ========== Poet Life Events (10 new poets) ==========
-- Bai Juyi
INSERT INTO poet_life_events (poet_id, year, age, title, description, event_type, delay_multiplier) VALUES
(6, 772, 0, '出生', '生于新郑书香门第', 'milestone', 1.0),
(6, 800, 28, '进士及第', '与元稹订交成终生挚友', 'achievement', 0.8),
(6, 807, 35, '长恨歌成', '创作《长恨歌》名动天下', 'achievement', 0.9),
(6, 815, 43, '贬谪江州', '因越职言事被贬江州司马', 'hardship', 1.4),
(6, 816, 44, '琵琶行成', '作《琵琶行》', 'achievement', 1.2),
(6, 822, 50, '筑白堤', '任杭州刺史疏浚西湖', 'achievement', 0.9),
(6, 846, 74, '安详离世', '病逝洛阳赠尚书右仆射', 'milestone', 1.0);

-- Meng Haoran
INSERT INTO poet_life_events (poet_id, year, age, title, description, event_type, delay_multiplier) VALUES
(7, 689, 0, '出生', '生于襄州襄阳书香之家', 'milestone', 1.0),
(7, 710, 21, '隐居鹿门', '隐居鹿门山开始田园生活', 'milestone', 1.0),
(7, 725, 36, '应试不第', '赴长安应试落第', 'hardship', 1.2),
(7, 728, 39, '王孟结识', '结识王维开创山水诗派', 'milestone', 0.9),
(7, 740, 51, '病逝襄阳', '因背疽发作而卒', 'milestone', 1.1);

-- Li Shangyin
INSERT INTO poet_life_events (poet_id, year, age, title, description, event_type, delay_multiplier) VALUES
(8, 813, 0, '出生', '生于怀州河内幼年丧父', 'milestone', 1.1),
(8, 837, 24, '进士及第', '才华初露', 'achievement', 0.8),
(8, 838, 25, '婚姻与党争', '娶王茂元女卷入牛李党争', 'hardship', 1.5),
(8, 851, 38, '丧妻之痛', '爱妻王氏病故作悼亡名篇', 'hardship', 1.5),
(8, 858, 45, '郁郁而终', '病逝于郑州年仅四十五', 'milestone', 1.3);

-- Du Mu
INSERT INTO poet_life_events (poet_id, year, age, title, description, event_type, delay_multiplier) VALUES
(9, 803, 0, '出生', '生于京兆万年名门', 'milestone', 1.0),
(9, 825, 22, '连中两科', '同年进士及第又制策登科', 'achievement', 0.8),
(9, 833, 30, '扬州风流', '十年一觉扬州梦', 'milestone', 0.9),
(9, 842, 39, '外放黄州', '因党争被排挤出京', 'hardship', 1.3),
(9, 852, 49, '病逝长安', '官至中书舍人病逝', 'milestone', 1.2);

-- Xin Qiji
INSERT INTO poet_life_events (poet_id, year, age, title, description, event_type, delay_multiplier) VALUES
(10, 1140, 0, '出生', '生于金国统治下的济南', 'milestone', 1.0),
(10, 1161, 21, '聚众抗金', '率两千人义军擒叛徒南归', 'achievement', 0.8),
(10, 1176, 36, '创建飞虎军', '在湖南创建南宋劲旅', 'achievement', 0.9),
(10, 1181, 41, '罢官闲居', '被弹劾罢官闲居十年', 'hardship', 1.5),
(10, 1203, 63, '晚年起复', '年过六旬再次被起用', 'achievement', 1.2),
(10, 1207, 67, '壮志未酬', '病逝前大呼杀贼数声', 'milestone', 1.3);

-- Lu You
INSERT INTO poet_life_events (poet_id, year, age, title, description, event_type, delay_multiplier) VALUES
(11, 1125, 0, '出生', '生于越州山阴时值北宋末年', 'milestone', 1.1),
(11, 1144, 19, '沈园之痛', '娶唐婉后被迫离异终生遗憾', 'hardship', 1.3),
(11, 1153, 28, '秦桧黜落', '应试被秦桧黜落仕途受阻', 'hardship', 1.4),
(11, 1172, 47, '前线抗金', '在南郑前线度过最豪迈时光', 'achievement', 0.9),
(11, 1189, 64, '罢官闲居', '退居山阴长达二十载', 'hardship', 1.4),
(11, 1210, 85, '临终示儿', '王师北定中原日家祭无忘告乃翁', 'milestone', 1.5);

-- Tao Yuanming
INSERT INTO poet_life_events (poet_id, year, age, title, description, event_type, delay_multiplier) VALUES
(12, 365, 0, '出生', '生于浔阳柴桑', 'milestone', 1.0),
(12, 405, 40, '不为五斗米折腰', '任彭泽令八十余日挂印辞官', 'milestone', 1.0),
(12, 421, 56, '桃花源记成', '描绘理想中的世外乐土', 'achievement', 0.9),
(12, 427, 62, '安然离世', '病逝于浔阳家中', 'milestone', 1.0);

-- Cao Zhi
INSERT INTO poet_life_events (poet_id, year, age, title, description, event_type, delay_multiplier) VALUES
(13, 192, 0, '出生', '曹操第四子自幼聪颖', 'milestone', 1.0),
(13, 210, 18, '铜雀台赋', '才华震惊四座', 'achievement', 0.8),
(13, 220, 28, '世子之争败', '曹操去世曹丕继位开始迫害', 'hardship', 1.5),
(13, 223, 31, '七步诗传说', '煮豆燃豆萁豆在釜中泣', 'hardship', 1.5),
(13, 232, 40, '郁郁而终', '在屡次徙封下病逝', 'milestone', 1.3);

-- Ouyang Xiu
INSERT INTO poet_life_events (poet_id, year, age, title, description, event_type, delay_multiplier) VALUES
(14, 1007, 0, '出生', '生于吉州四岁丧父', 'milestone', 1.1),
(14, 1030, 23, '进士及第', '受知于钱惟演', 'achievement', 0.8),
(14, 1036, 29, '贬谪夷陵', '为范仲淹辩护被贬', 'hardship', 1.3),
(14, 1045, 38, '醉翁亭记成', '贬谪滁州写下千古名篇', 'achievement', 1.1),
(14, 1057, 50, '主考取士', '录取苏轼苏辙曾巩等', 'achievement', 0.8),
(14, 1072, 65, '病逝颍州', '致仕后病逝谥号文忠', 'milestone', 1.0);

-- Tang Yin
INSERT INTO poet_life_events (poet_id, year, age, title, description, event_type, delay_multiplier) VALUES
(15, 1470, 0, '出生', '生于苏州商人之家', 'milestone', 1.0),
(15, 1497, 27, '中解元', '应天府乡试第一名', 'achievement', 0.8),
(15, 1499, 29, '科场案', '卷入舞弊案下狱前程尽毁', 'hardship', 1.5),
(15, 1514, 44, '佯狂脱身', '察觉宁王不臣之心佯狂裸奔', 'hardship', 1.4),
(15, 1524, 54, '病逝桃花庵', '别人笑我太疯癫我笑他人看不穿', 'milestone', 1.2);

-- ========== Poems (sample poems for new poets) ==========
INSERT INTO poems (id, poet_id, title, content, year_written, context, translation, tags) VALUES
(1, 6, '赋得古原草送别', '离离原上草，一岁一枯荣。野火烧不尽，春风吹又生。远芳侵古道，晴翠接荒城。又送王孙去，萋萋满别情。', 788, '少年时期应试之作', '原野上的青草茂盛又枯萎，野火无法将其烧尽，春风一吹又获新生。', '["送别","咏物"]'),
(2, 6, '琵琶行', '浔阳江头夜送客，枫叶荻花秋瑟瑟。主人下马客在船，举酒欲饮无管弦。醉不成欢惨将别，别时茫茫江浸月。忽闻水上琵琶声，主人忘归客不发。', 816, '贬谪江州期间所作', '在浔阳江边送别朋友，忽然听到水上琵琶声，被深深打动。', '["叙事","音乐"]'),
(3, 7, '春晓', '春眠不觉晓，处处闻啼鸟。夜来风雨声，花落知多少。', 720, '隐居鹿门山时感春之作', '春天的早晨睡到自然醒，处处鸟鸣。昨夜风雨中不知多少花朵凋落。', '["山水","田园"]'),
(4, 7, '过故人庄', '故人具鸡黍，邀我至田家。绿树村边合，青山郭外斜。开轩面场圃，把酒话桑麻。待到重阳日，还来就菊花。', 730, '拜访友人田庄', '老朋友准备了丰盛的农家饭请我去做客。绿树青山环绕村庄，开窗对着打谷场。', '["田园","友情"]'),
(5, 8, '锦瑟', '锦瑟无端五十弦，一弦一柱思华年。庄生晓梦迷蝴蝶，望帝春心托杜鹃。沧海月明珠有泪，蓝田日暖玉生烟。此情可待成追忆，只是当时已惘然。', 850, '晚年悼亡追忆之作', '锦瑟的五十根琴弦，每一根都让人追忆逝去的年华。此情此景令人惘然若失。', '["爱情","悼亡"]'),
(6, 8, '夜雨寄北', '君问归期未有期，巴山夜雨涨秋池。何当共剪西窗烛，却话巴山夜雨时。', 848, '在蜀中寄给北方友人', '你问我何时归来，我说不准。巴山夜雨涨满秋池，何时能一起在西窗下共话今夜的雨。', '["思乡","友情"]'),
(7, 9, '清明', '清明时节雨纷纷，路上行人欲断魂。借问酒家何处有，牧童遥指杏花村。', 845, '任池州刺史时所作', '清明时节细雨纷纷，路上行人心情落寞。问牧童哪里有酒家，他指向远处的杏花村。', '["节令","写景"]'),
(8, 9, '泊秦淮', '烟笼寒水月笼沙，夜泊秦淮近酒家。商女不知亡国恨，隔江犹唱后庭花。', 833, '夜泊秦淮河畔', '烟雾笼罩寒水月光照沙滩，夜泊秦淮河边听到歌女在唱亡国之音。', '["咏史","怀古"]'),
(9, 10, '青玉案·元夕', '东风夜放花千树，更吹落，星如雨。宝马雕车香满路。凤箫声动，玉壶光转，一夜鱼龙舞。', 1175, '元宵节观灯有感', '元宵夜东风吹开千万树花灯，烟花如星雨般落下。华美的车马使街道香气弥漫。', '["节令","豪放"]'),
(10, 11, '示儿', '死去元知万事空，但悲不见九州同。王师北定中原日，家祭无忘告乃翁。', 1210, '临终绝笔诗', '我知道死后一切都成空，唯一悲伤是看不到国家统一。等到王师收复中原，祭祀时别忘了告诉你们的父亲。', '["爱国","临终"]'),
(11, 12, '饮酒·其五', '结庐在人境，而无车马喧。问君何能尔？心远地自偏。采菊东篱下，悠然见南山。山气日夕佳，飞鸟相与还。', 410, '归隐田园后作', '在人世间居住却没有车马喧嚣。问我为何能做到？心远离尘世自然觉得偏僻。', '["田园","哲理"]'),
(12, 13, '七步诗', '煮豆持作羹，漉豉以为汁。萁在釜下燃，豆在釜中泣。本是同根生，相煎何太急。', 223, '曹丕令七步内成诗', '用豆秸煮豆做羹，豆在锅中哭泣。本是同根而生，为何煎熬得如此急迫。', '["讽刺","兄弟"]'),
(13, 14, '蝶恋花', '庭院深深深几许？杨柳堆烟，帘幕无重数。玉勒雕鞍游冶处，楼高不见章台路。雨横风狂三月暮，门掩黄昏，无计留春住。', 1045, '贬谪滁州时作', '庭院深深到底有多深？杨柳如烟帘幕重重。春去无处挽留。', '["婉约","抒情"]'),
(14, 15, '桃花庵歌', '桃花坞里桃花庵，桃花庵里桃花仙。桃花仙人种桃树，又摘桃花换酒钱。酒醒只在花前坐，酒醉还来花下眠。半醒半醉日复日，花落花开年复年。', 1520, '在苏州桃花庵作', '桃花坞里有一座桃花庵，桃花庵里住着桃花仙人。种桃树摘桃花换酒钱。', '["隐逸","自嘲"]');

SELECT setval('poems_id_seq', 14);
