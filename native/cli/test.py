import sys

list0 = [('0', 'zéro')]
list1_9 = [('1', 'un'),
           ('2', 'deux'),
           ('3', 'trois'),
           ('4', 'quatre'),
           ('5', 'cinq'),
           ('6', 'six'),
           ('7', 'sept'),
           ('8', 'huit'),
           ('9', 'neuf')]
list10_99 = [('10', 'dix'),
             ('11', 'onze'),
             ('12', 'douze'),
             ('13', 'treize'),
             ('14', 'quatorze'),
             ('15', 'quinze'),
             ('16', 'seize'),
             ('17', 'dix-sept'),
             ('18', 'dix-huit'),
             ('19', 'dix-neuf'),
             ('20', 'vingt'),
             ('21', 'vingt et un'),
             ('22', 'vingt-deux'),
             ('23', 'vingt-trois'),
             ('24', 'vingt-quatre'),
             ('25', 'vingt-cinq'),
             ('26', 'vingt-six'),
             ('27', 'vingt-sept'),
             ('28', 'vingt-huit'),
             ('29', 'vingt-neuf'),
             ('30', 'trente'),
             ('31', 'trente et un'),
             ('32', 'trente-deux'),
             ('33', 'trente-trois'),
             ('34', 'trente-quatre'),
             ('35', 'trente-cinq'),
             ('36', 'trente-six'),
             ('37', 'trente-sept'),
             ('38', 'trente-huit'),
             ('39', 'trente-neuf'),
             ('40', 'quarante'),
             ('41', 'quarante et un'),
             ('42', 'quarante-deux'),
             ('43', 'quarante-trois'),
             ('44', 'quarante-quatre'),
             ('45', 'quarante-cinq'),
             ('46', 'quarante-six'),
             ('47', 'quarante-sept'),
             ('48', 'quarante-huit'),
             ('49', 'quarante-neuf'),
             ('50', 'cinquante'),
             ('51', 'cinquante et un'),
             ('52', 'cinquante-deux'),
             ('53', 'cinquante-trois'),
             ('54', 'cinquante-quatre'),
             ('55', 'cinquante-cinq'),
             ('56', 'cinquante-six'),
             ('57', 'cinquante-sept'),
             ('58', 'cinquante-huit'),
             ('59', 'cinquante-neuf'),
             ('60', 'soixante'),
             ('61', 'soixante et un'),
             ('62', 'soixante-deux'),
             ('63', 'soixante-trois'),
             ('64', 'soixante-quatre'),
             ('65', 'soixante-cinq'),
             ('66', 'soixante-six'),
             ('67', 'soixante-sept'),
             ('68', 'soixante-huit'),
             ('69', 'soixante-neuf'),
             ('70', 'soixante-dix'),
             ('71', 'soixante-et-onze'),
             ('72', 'soixante-douze'),
             ('73', 'soixante-treize'),
             ('74', 'soixante-quatorze'),
             ('75', 'soixante-quinze'),
             ('76', 'soixante-seize'),
             ('77', 'soixante-dix-sept'),
             ('78', 'soixante-dix-huit'),
             ('79', 'soixante-dix-neuf'),
             ('80', 'quatre-vingts'),
             ('81', 'quatre-vingt-un'),
             ('82', 'quatre-vingt-deux'),
             ('83', 'quatre-vingt-trois'),
             ('84', 'quatre-vingt-quatre'),
             ('85', 'quatre-vingt-cinq'),
             ('86', 'quatre-vingt-six'),
             ('87', 'quatre-vingt-sept'),
             ('88', 'quatre-vingt-huit'),
             ('89', 'quatre-vingt-neuf'),
             ('90', 'quatre-vingt-dix'),
             ('91', 'quatre-vingt-onze'),
             ('92', 'quatre-vingt-douze'),
             ('93', 'quatre-vingt-treize'),
             ('94', 'quatre-vingt-quatorze'),
             ('95', 'quatre-vingt-quinze'),
             ('96', 'quatre-vingt-seize'),
             ('97', 'quatre-vingt-dix-sept'),
             ('98', 'quatre-vingt-dix-huit'),
             ('99', 'quatre-vingt-dix-neuf')]


def fr1_9(pref_digit, pref_word, p):
    for (digit, word) in list1_9:
        p(pref_digit + digit, pref_word + word)


def fr10_99(pref_digit, pref_word, p):
    for (digit, word) in list10_99:
        p(pref_digit + digit, pref_word + word)


def fr1_99(pref_digit, pref_word, p):
    fr1_9(pref_digit, pref_word, p)
    fr10_99(pref_digit, pref_word, p)


def fr01_99(pref_digit, pref_word, p):
    fr1_9(pref_digit + '0', pref_word, p)
    fr10_99(pref_digit, pref_word, p)


def fr100_999(pref_digit, pref_word, p):
    p(pref_digit + '100', pref_word + 'cent')
    fr01_99(pref_digit + '1', pref_word + 'cent ', p)
    fr1_9(pref_digit, pref_word, lambda d, w: fr01_99(d, w + ' cent ', p))


def fr1_999(pref_digit, pref_word, p):
    fr1_99(pref_digit, pref_word, p)
    fr100_999(pref_digit, pref_word, p)


def fr001_999(pref_digit, pref_word, p):
    fr01_99(pref_digit + '0', pref_word, p)
    fr100_999(pref_digit, pref_word, p)


def fr1000_999999(pref_digit, pref_word, p):
    p(pref_digit + '1000', pref_word + 'mille')
    fr001_999(pref_digit + '1', pref_word + 'mille ', p)
    fr1_999(pref_digit, pref_word, lambda d, w: fr001_999(d, w + ' mille ', p))


def fr000001_999999(pref_digit, pref_word, p):
    fr001_999(pref_digit + '000', pref_word, p)
    fr1000_999999(pref_digit + '00', pref_word, p)


def fr1_999999(pref_digit, pref_word, p):
    fr1_999(pref_digit, pref_word, p)
    fr1000_999999(pref_digit, pref_word, p)


def fr1000000_999999999(pref_digit, pref_word, p):
    p(pref_digit + '1000000', pref_word + 'million')
    fr001_999(pref_digit + '1', pref_word + 'millions ', p)
    fr1_999(pref_digit, pref_word, lambda d, w: fr001_999(d, w + ' millions ', p))


f = fr1_999999
if len(sys.argv) > 1:
    if sys.argv[1] == '100':
        f = fr1_99
    elif sys.argv[1] == '1000':
        f = fr1_999
    elif sys.argv[1] == '1000000':
        f = fr1_999999

f('', '', lambda d, w: print(w + '\t' + d))
