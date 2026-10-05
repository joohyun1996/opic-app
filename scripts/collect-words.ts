import { mkdir, readFile, writeFile } from 'node:fs/promises'
import type { WordInput } from './lib/word-export'

const englishUrl = 'https://raw.githubusercontent.com/first20hours/google-10000-english/master/google-10000-english-usa-no-swears.txt'
const chineseUrl = (level: number) => `https://raw.githubusercontent.com/drkameleon/complete-hsk-vocabulary/main/wordlists/exclusive/old/${level}.json`
const sourceDirectory = new URL('../exports/source/', import.meta.url)

type Source = {
  metadata: {
    urls: string[]
    collectedAt: string
    rawCounts: number[]
    duplicates: string[]
    multipleForms: string[]
    dictionaryFailures: string[]
    authoring?: string
    selectionVersion?: number
    excluded?: { word: string; reason: string }[]
  }
  words: WordInput[]
}
type ChineseEntry = {
  simplified: string
  pos?: string[]
  forms: { transcriptions: { pinyin: string }; meanings: string[] }[]
}

async function fetchSource(url: string): Promise<Response> {
  for (let attempt = 0; attempt < 4; attempt++) {
    let response: Response
    try {
      response = await fetch(url, { signal: AbortSignal.timeout(30000) })
    } catch (error) {
      if (attempt === 3) throw new Error(`네트워크 접근 실패: ${url} (${error instanceof Error ? error.message : String(error)})`)
      console.log(`일시적 연결 오류, 재시도 ${attempt + 1}/3: ${url}`)
      await new Promise(resolve => setTimeout(resolve, 2000))
      continue
    }
    if (response.status !== 429 && response.status < 500) return response
    if (attempt === 3) throw new Error(`출처 응답 실패: ${response.status} ${url}`)
    await new Promise(resolve => setTimeout(resolve, 2000 * (attempt + 1)))
  }
  throw new Error(`출처 조회 실패: ${url}`)
}

async function load(language: string): Promise<Source | undefined> {
  try {
    return JSON.parse(await readFile(new URL(`words-${language}.json`, sourceDirectory), 'utf8')) as Source
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') return undefined
    throw error
  }
}

async function save(language: string, source: Source) {
  await mkdir(sourceDirectory, { recursive: true })
  await writeFile(new URL(`words-${language}.json`, sourceDirectory), `${JSON.stringify(source, null, 2)}\n`)
}

function emptySource(urls: string[], rawCounts: number[]): Source {
  return {
    metadata: { urls, collectedAt: new Date().toISOString(), rawCounts, duplicates: [], multipleForms: [], dictionaryFailures: [] },
    words: [],
  }
}

async function collectChinese() {
  const previous = await load('zh')
  if (previous?.words.length === 4991) {
    console.log(`중국어 기존 수집 결과 유지: ${previous.words.length}개`)
    return
  }
  const translations = new Map(previous?.words.map(word => [word.word, word]))
  const source = emptySource([1, 2, 3, 4, 5, 6].map(chineseUrl), [])
  const seen = new Set<string>()
  for (let level = 1; level <= 6; level++) {
    const response = await fetchSource(chineseUrl(level))
    if (!response.ok) throw new Error(`HSK ${level} 조회 실패: ${response.status}`)
    const entries = await response.json() as ChineseEntry[]
    source.metadata.rawCounts.push(entries.length)
    for (const entry of entries) {
      const word = entry.simplified.trim().toLowerCase()
      if (seen.has(word)) {
        source.metadata.duplicates.push(word)
        continue
      }
      seen.add(word)
      if (entry.forms.length > 1) source.metadata.multipleForms.push(word)
      const form = entry.forms[0]
      if (!word || !form?.transcriptions.pinyin || !form.meanings.length) {
        throw new Error(`중국어 원본 필드 누락: ${word}`)
      }
      source.words.push({
        language: 'zh', word, phonetic: form.transcriptions.pinyin,
        meaningKo: translations.get(word)?.meaningKo ?? '',
        meaningEn: form.meanings.join('; '), example: null, exampleKo: null,
        level, category: `HSK${level}`, partOfSpeech: entry.pos?.[0] ?? null, collocations: [],
      })
    }
  }
  await save('zh', source)
  console.log(`중국어 수집: ${source.words.length}개, 중복 ${source.metadata.duplicates.length}개, 복수 독음 ${source.metadata.multipleForms.length}개`)
}

// 제외 기준은 원본 전체에 원형이 있는지 확인한다. 일반 명사로도 쓰이는
// 단어(apple, brown 등)는 일반적인 뜻으로 유지한다.
const abbreviations = new Set(`
font disk circuit weblog printable offline converter node unsubscribe python ringtone spam gamma phentermine graphics desktop interface pics homepage pm re jan info de feb mar apr jun jul aug sep oct nov dec non cd tv pc ii iii iv
fax rss id co faq et pro st url auto tech en ad ny eur usr dc mon pre sat zip ok
ms fri hi wed super cnet ltd los hp inc eg ip tue pa thu anti tel tx ie el ma est
mac gmt max xml bin md fl mb mr multi prev ads il int mm mini usb ed php etc
msn las le min isbn az un pst mi sub th dr kb dev vol pp na os dvds ex au fi nc
llc sec po va ibm rd sc mid se di km del ga ac ft admin cds im vs ar mo sa xp
kong sitemap lab cvs eu des cc lcd wa ave dj cm wi ct da pic aids stats nj hr em
rw vhs pubmed ne du con ups nt es bytes null gb bc pr fr aa var mt beta gratis
doc oz usd mg les math ch sd devel rs alpha avg rom src faqs hiv pda dsl zum dna
diff sql specs ss ap nm mn nd gps op acc tn gnu aol ce ab utc der verzeichnis cam
ram tm sp nh mysql fm pdt ref db ph ia pt psp ha ds ea und demo lg nfl nw ff iso
misc holdem vat spyware ps const ky dont br lo ml res char cs que voip fig sf kg
ut nasa si css mc qty uniprotkb lp bio pcs von ag vi sci edt pmid sin ba para cr
pg ee medline ing ks ftp med sw hd abc livecam er jr biz gcc asp par nv semi su
exp debian cpu sr nr lbs lol mp mph def li nl ethernet postposted ya nba epa tr
cialis bb nz org hist inter firefox av reg tion wiki nsw pci ra mod rc mpeg cst
http ceo twiki ec rep dicke mit rm pdas sri trans api config cf vt urw lan sms
nec foto gm ri rt cp telecom dd aud pl crm rf ak fe td amp sb ah sm usc trembl
blvd amd wv ns ja bs hrs bi compaq cal ta img rpm deutsch nutten tvs mhz lat
meta packard gi sur gnome rev ll ieee ho corp gt sh ae nyc hs rv struct pi ai ot
mrs yr app inkjet ic plugin temp intro zus ent mx gr xhtml ext ts ge ncaa phd ng
pe pentium tt aka tee goto xl cad tcp dv dir rip zope gmbh buf ld eco indie msgid
sap suse mf msgstr mw adipex nu ict dp ou xi sku ht za ve kelkoo pts rh rrp fg
ooo hz ipaq bk nhs aye ste ment col dx sk biol yu sq oc aj treo tex cia dod wp
neo cons om nat tp jm dpi gis loc vp expo cn newbie gui ver rn dis andale cg
soma ser ascii href wifi fwd aus endif hwy nam ix gdp hotmail lit una ada tb yrs
foo gba bm howto sig str gotta vip mardi yea rj soc sync telephony ka voyeurweb
dem wav gratuit rp eh tba sie usgs hc italiano rca fp hydrocodone gst bon mailto
promo jj mas len kai dom dans viii metadata ev dept cos uzbekistan pac rl erp gl
ui dh vpn fcc eds ro df acdbentity whats rehab zshops ambien sans irs cdt ez pf
uw eau bd scuba mil divx eval muze gmc hh adsl uh prix fd bo asn listprice libs
undo wm pk sagem knowledgestorm ppm apt exec inf eos vcr uri electro psi pct wb
smilies vids sn qld pas dk issn sas fu mysimon oecd quad firewire mods vsnet msie
wn ccd sv zu llp boc dg vg filme craps fo tmp alot fin io uni ul ol js pn diffs
casa mu nvidia runtime incl ala hq propecia nbc wt flex mv mpg carb cas cio dow
rb upc dui pvc blah mime feof tions usda urls enb gg og ko til invision emacs wto
hay ww sparc gd isa kinda grad pix mic basename bw mj lf ata nil bufing nato cop
mpegs wc sbjct mai hk abu ty italia cj mtv leu changelog qc pgp aw advert tf pj
cw wr fioricet rg bl vc wx paxil ntsc apnic sic usps bg seq conf wma cir hu acm
kw ips dsc ware mia wan keno gtk voyuer ru mrna ira sen erotica dts qt cdna pod
wu lu dat soa smtp lm notre firmware bool cho bbs ind qui intl adware zoloft
ultram cz hl ob telecharger ids gzip ctrl lightbox mediawiki www com pdf html
mp3 html javascript java linux unix bluetooth broadband permalink trackback
webmaster webpage webshots webshots screensaver screensavers webcam webcams
namespace workstation toolbar firewall modem modems motherboard toolkit
`.trim().split(/\s+/))
const properNames = new Set(`
hewlett flickr jamaica britney manhattan katrina islam portuguese medicare titten skype gamecube
pete todd rachel harrison penn victor
pierre oakland colombia berkeley norfolk delhi nepal zimbabwe juan barcelona
melissa moscow doug montgomery louisville monica lynn stanley brooklyn
liverpool madrid norman madonna murphy justin danish florence arabic
kingston sandy salem luke kenneth julie janet helen lopez catherine chuck
dale newton queensland leeds raleigh blair beijing mario brunswick hudson
calgary newport prague bangladesh iceland morocco tommy springfield oliver
congo glen botswana newcastle brazilian josh solomon soviet vienna stanford
bahamas powell hampton bobby stuart salvador monroe tanzania easter jeremy
venice estonia christianity namibia christina ecuador colin samoa lauren
ashley victorian cambodia lithuania essex slovenia cruz baptist wallace
ruth arnold arlington bolivia kuwait emily plymouth sudan shakespeare
elvis latvia martha guatemala celtic jamie bangkok sara hughes jeffrey
brisbane oscar joan manitoba watson saddam bryan jerusalem fiji myers
saskatchewan geneva tucson jacob randy timothy dayton cameron reno donna
patricia eddie uganda perth syria klein harvey gregory glenn auckland
trinidad mcdonald midwest thanksgiving vietnamese andorra
marriott norton heather chrome solaris xanax
arabia spears
jeep benjamin
nigeria jews
mitsubishi indonesian
bradley fisher hebrew
lanka brad
palestinian phillips luxembourg turner
wordpress
katie
princeton
joel perry hispanic
albany
cole
panama indianapolis athens nissan yorkshire croatia venezuela norwegian honolulu
beatles thomson barnes malta dominican chad omaha yamaha rochester slovakia
barbados chrysler brussels charleston shanghai burton davidson scotia gibson
troy nicole latino ghana albuquerque
playstation americans indian mary pacific german iraq spanish eb ay ebay uk american usa john york canada english june october november march
september july yahoo san april august america texas february china london
washington california australia david france microsoft europe germany james
india michael florida paul friday sony google french japan africa monday
european sunday robert british tuesday saturday christmas england george
thursday wednesday virginia asian thomas chicago smith mexico christian paypal
nokia carolina italy william peter amazon ireland canadian chinese asia
richard japanese spain boston martin paris ohio mike georgia tom michigan
columbia angeles australian francisco colorado russian african bible vegas
chris lee charles illinois bob xbox steve ford zealand scott canon johnson
kansas santa israel jones arizona italian saint jim samsung louis joe
pennsylvania ipod motorola latin oregon houston ontario minnesota williams jesus
jackson russia seattle indiana harry wisconsin hawaii atlanta dakota dave
massachusetts diego hong iowa maryland scotland dallas tripadvisor alaska
netherlands korea kelly austin toronto missouri andrew joseph philadelphia brian
miami tennessee wales davis daniel brazil oklahoma dell intel ann henry christ
dan austria singapore phoenix cisco disney adobe bbc alabama utah panasonic
miller kentucky eric taylor victoria orlando tim maine don sydney louisiana
wilson irish stephen elizabeth greek jeff ben sweden mississippi connecticut
kevin jordan anderson tony nevada thailand atlantic edward matt iran costa
belgium denver ericsson hampshire arkansas britain jean jewish portland lewis
howard sam allen montana switzerland manchester czech cambridge idaho potter
oxford adam epinions jennifer antonio taiwan greece swiss sarah clark caribbean
nebraska orleans gary rome arab lincoln guinea alan jason catholic vancouver
jose jane pakistan kim baltimore charlotte bruce egypt hollywood wikipedia
norway vermont simon pittsburgh moore denmark patrick puerto delaware barbara
cleveland latina dutch poland detroit alexander toshiba nick birmingham vietnam
anne malaysia ryan philippines honda indonesia berlin siemens buffalo marshall
tampa susan adams alex hamilton portugal lawrence roman jay oracle matthew
anthony nintendo franklin rob melbourne wyoming argentina finland morgan
columbus apache andy valentine hilton ken douglas christopher epson madison jon
wayne harris tiffany toyota larry sierra rhode steven greg scottish richmond
sacramento ron russell clinton alberta cincinnati roger mexican lisa korean
thompson pat dublin nelson mayor sean kent arthur ian walker dodge gordon bmw
ukraine rico phil nashville simpson milwaukee kerry keith linda ross chile anna
maria jonathan nova edinburgh campbell hungary philips iraqi jessica terry
amsterdam stewart jerry bulgaria rick laura tokyo nikon donald thai fred
afghanistan roberts billy craig johnny nancy cyprus eve carter marc saudi cuba
kate chevrolet karen albert jimmy graham bristol margaret samuel wright barry
warren marie kenya charlie quebec dennis mars francis philip swedish collins
broadway glasgow islamic parker lebanon kennedy cooper vincent titans rio
montreal duke walter carl mitchell rica ralph warner morris eminem minneapolis
memphis harvard roy ottawa peru evans nike alice latinas mercedes holland
jefferson michelle amy aaron romania turkish murray durham muslim neil netscape
robin robinson jacksonville sharon olympic israeli israel windsor zambia sims
sussex alfred carey danny rebecca bosnia lloyd brighton ellis leonard riverside
raymond sullivan lancaster julia karl bermuda amanda symantec spencer wichita
nasdaq uruguay hungarian mazda tyler henderson britannica ellen beverly duncan
hart bernard midlands floyd ronald nicholas fujitsu arctic frederick medicaid
welsh haiti petersburg powerpoint belarus providence ethiopia holmes chester
shaw itunes darwin bali judy volkswagen milton sandra adelaide monaco emirates
belize volvo minolta porter zen mozambique benz earl diane andrea sheffield
bailey wellington lexmark bulgarian guam eugene bennett joshua armenia grande
armstrong aurora milan christine clarke marilyn findlaw claire carroll finnish
mediterranean devon helena marion egyptian tunisia metallica bahrain betty
queens lucas honduras harper holly hartford marcus nicaragua diana lindsay mesa
pamela andrews jesse logitech surrey belkin blackberry macedonia brandon
winston kazakhstan hawaiian fbi qatar porsche cayman jaguar rand hopkins cohen
kathy chen emma eva cornell chan beth bradford chelsea reynolds jill lucia
savannah boulder myanmar harold somerset antigua nottingham madagascar nathan
tobago richardson hindu albania lucy hans saturn edmonton pokemon lexington
vernon worldcat leslie casio deutsche ana monte algeria luis cardiff cornwall
leone alto bloomberg mauritius verde lauderdale hitachi batman coleman hugh
jake anaheim luther palmer derby sao pontiac munich oman azerbaijan annie derek
harley paraguay avon peterson shannon walt palestine cadillac malawi dana curtis
belfast niagara warcraft lexus heath maui angola jenny yemen expedia shakira
yale newfoundland stephanie louise kenny owen yukon singh gilbert ferrari brook
auburn hyundai angela xerox nextel rwanda papua liz burlington kruger aruba
edgar dubai asus pierce allan fuji leon sally tracy bedford mustang calvin
abraham senegal wendy mumbai eden champagne mongolia reid robertson belle rosa
hamburg cindy marvel yen crawford serbia griffin guyana preston chevy holocaust
cameroon nirvana fairfield lafayette jackie dover adrian vatican gibraltar
aberdeen dylan burke kyle clara hayes garcia cingular alexandria moses johns
teddy julian pam gale wesley brunei slovak stan worcester logan springer baghdad
concord walnut lance liberia sherman hansen mali yugoslavia caroline yang
greensboro tulsa casey simpsons lou elliott fraser tahoe subaru deutschland
smithsonian blake dee travis niger forbes mae moldova gerald mariah joyce
deborah marco gabriel looksmart isaac naples erik prozac newark paso westminster
apollo persian greene grenada sanyo somalia carlo bryant stockholm tamil garmin
richards carmen southampton istanbul sega portsmouth meyer barbie kurt roland
huntington syracuse michel evanescence maldives antarctica canberra kirk wiley
johnston frankfurt daisy halifax morrison viking myrtle budapest bhutan
liechtenstein allah hugo wagner cologne newman ricky laos robbie swaziland
january december
`.trim().split(/\s+/))
const formatTerms = new Set(`
www com pdf html php url rpg mp3 dvd xml sql css javascript
vbulletin citysearch shareware showtimes dealtime freeware
jelsoft lycos zdnet oclc espn reuters macintosh mozilla kodak olympus
expansys nascar verizon gamespot myspace webster meetup ampland shopzilla
macromedia mastercard kijiji audi freebsd acer thumbzilla pichunter
valium adidas jpeg suzuki phpbb thinkpad techrepublic powerseller frontpage
asin utils
bizrate viewpicture
thru
phys chem midi
lite cant
sept spec comp
proc
lang
univ
stat
dist
prot
comm
poly
gras
celebs
perl
photoshop prostores scsi starsmerchant sublimedirectory findarticles
thumbzilla pichunter pix hdtv
approx
`.trim().split(/\s+/))

const spamTerms = new Set(`
interracial naked deviant porn porno pornography erotic erotica xxx hentai
sexcam sexcams camgirl camgirls webcamgirl webcamgirls escort escorts
adultfriendfinder phentermine cialis viagra levitra hydrocodone fioricet
levitra webcamsex hardcore nude nudity topless fetish incest shemale
pantyhose sucking swingers travesti transexuales transexual thong nudist
spank flashers beastality redhead xxxporn pornstars lesbianporn
tramadol
thehun
blackjack roulette
mistress
lesbian
transsexual
butts
`.trim().split(/\s+/))

const irregularRedundancies = new Map([
  ['grew', 'grow'], ['gloves', 'glove'], ['bleeding', 'bleed'],
  ['grown', 'grow'],
  ['limousines', 'limousine'],
  ['teeth', 'tooth'], ['hung', 'hang'],
  ['sold', 'sell'], ['laid', 'lay'], ['struck', 'strike'],
  ['higher', 'high'], ['meant', 'mean'], ['worse', 'bad'],
  ['gotten', 'get'],
  ['finest', 'fine'],
  ['stockings', 'stocking'],
  ['dying', 'die'], ['harder', 'hard'],
  ['viruses', 'virus'],
  ['columnists', 'columnist'],
  ['undertaken', 'undertake'],
  ['closest', 'close'],
  ['shorter', 'short'],
  ['dealt', 'deal'],
  ['buses', 'bus'], ['housewives', 'housewife'],
  ['longest', 'long'],
  ['earliest', 'early'],
  ['stolen', 'steal'],
  ['hottest', 'hot'], ['fastest', 'fast'], ['lying', 'lie'],
  ['nearest', 'near'], ['mice', 'mouse'],
  ['bestsellers', 'bestseller'],
  ['safer', 'safe'],
  ['oldest', 'old'], ['younger', 'young'], ['newer', 'new'],
  ['deeper', 'deep'], ['cheapest', 'cheap'], ['bigger', 'big'],
  ['knives', 'knife'], ['wives', 'wife'], ['leaves', 'leaf'],
  ['began', 'begin'], ['begun', 'begin'], ['broke', 'break'], ['broken', 'break'],
  ['brought', 'bring'], ['bought', 'buy'], ['caught', 'catch'], ['chose', 'choose'],
  ['chosen', 'choose'], ['came', 'come'], ['did', 'do'], ['done', 'do'],
  ['drew', 'draw'], ['drawn', 'draw'], ['drank', 'drink'], ['drunk', 'drink'],
  ['drove', 'drive'], ['driven', 'drive'], ['ate', 'eat'], ['eaten', 'eat'],
  ['fell', 'fall'], ['fallen', 'fall'], ['felt', 'feel'], ['fought', 'fight'],
  ['found', 'find'], ['flew', 'fly'], ['flown', 'fly'], ['forgot', 'forget'],
  ['forgotten', 'forget'], ['gave', 'give'], ['given', 'give'], ['went', 'go'],
  ['gone', 'go'], ['had', 'have'], ['heard', 'hear'], ['kept', 'keep'],
  ['knew', 'know'], ['known', 'know'], ['left', 'leave'], ['lost', 'lose'],
  ['made', 'make'], ['met', 'meet'], ['paid', 'pay'], ['ran', 'run'],
  ['said', 'say'], ['saw', 'see'], ['seen', 'see'], ['sent', 'send'],
  ['sat', 'sit'], ['slept', 'sleep'], ['spoke', 'speak'], ['spoken', 'speak'],
  ['spent', 'spend'], ['stood', 'stand'], ['took', 'take'], ['taken', 'take'],
  ['taught', 'teach'], ['told', 'tell'], ['thought', 'think'], ['threw', 'throw'],
  ['thrown', 'throw'], ['understood', 'understand'], ['wore', 'wear'],
  ['worn', 'wear'], ['won', 'win'], ['wrote', 'write'],
])

const independentForms = new Set(`
news series species means physics economics politics mathematics statistics
clothes goods graphics headquarters customs savings remains thanks
building meeting learning training shopping shipping housing clothing fishing
following
writing reading understanding hearing feeling painting ceiling flooring
morning evening beginning nursing accounting manufacturing engineering
advertising marketing recording lighting parking dining funding wedding
savings earnings proceedings surroundings belongings glasses pants shorts
jeans scissors trousers media data analysis basis crisis thesis emphasis
business address access process success less glass class grass pass across
plus status focus virus campus bonus apparatus canvas gas bus lens chess
stress progress congress dress press mess loss witness fitness illness
unless tennis promise purpose purchase practice release increase surprise
exercise produce refuse abuse excuse please case base use close rose house
mouse wise otherwise whose these those his this as was yes us is
advanced experienced interested interesting leading outstanding working
related associated dedicated limited united married retired disabled
exciting amazing willing missing used friendly lovely likely early daily
weekly monthly yearly only holy ugly family assembly supply reply apply
`.trim().split(/\s+/))
const shortWords = new Set(`
the of and to a in for on by i you it not or be at as all new an we can us if my
but our one do no he up may out use any see so his who now get how me its top day
two go buy her add she set map way off car own end him per big law art old why
low man job too box gay air yes hot say tax let act red key few age pay war yet
sun run net put try log fun lot ask due ago via bad far oil bit oh bay bar dog
gas six bid inn win bed sea cut kit boy son van pop hit eye fee aid fat fan ten
cat die pet guy cup fit ice bus bag nor bug tag mix fix ray dry spa mom row eat
aim tip ski fly hey tea sky toy hip dot cap ink pin raw hat wet fox arm pub hop
gun kid pan sum oak sir spy sit wow tab leg gap era jet mad pen joy ill lay bet
dad ear tie toe den lie pad rod sad pot pee egg jam arc ion ban odd cry zoo hub
ace sue flu rap tin rat cod gel thy pal um ye mat gym yo tan pie bow cab dam tub
ash tap bee pit ton bra cow doe rid bat lip dig rim pig duo fog fur tar rug ham
jar wax nut bye lap ant
`.trim().split(/\s+/))
// 확실히 쉬운 항목만 명시한다. 애매한 항목은 포함한다.
const easyWords = new Set(`
the of and to a in for on by i you it not or be at as all new an we can us if my
but our one do no he up may out use any see so his who now get how me its top day
two go buy her add she set map way off car own end him per big law art old why
low man job too box gay air yes hot say tax let act red key few age pay war yet
sun run net put try log fun lot ask due ago via bad far oil bit oh bay bar dog
gas six bid inn win bed sea cut kit boy son van pop hit eye fee aid fat fan ten
cat die pet guy cup fit ice bus bag nor bug tag mix fix ray dry spa mom row eat
aim tip ski fly hey tea sky toy hip dot cap ink pin raw hat wet fox arm pub hop
gun kid pan sum oak sir spy sit wow tab leg gap era jet mad pen joy ill lay bet
dad ear tie toe den lie pad rod sad pot pee egg jam arc ion ban odd cry zoo hub
ace sue flu rap tin rat cod gel thy pal um ye mat gym yo tan pie bow cab dam tub
ash tap bee pit ton bra cow doe rid bat lip dig rim pig duo fog fur tar rug ham
jar wax nut bye lap ant
about above after again against ago almost also always among another answer
anyone anything around away back bad ball bank basic beautiful because become
bed before begin behind believe below between beyond bike bill bird black blue
body book born both bottle boy bread break breakfast bring brother brown build
building bus business busy call came camera can car card care carry case catch
change check child children city class clean clear close clothes coffee cold
college color come common company compare complete computer condition continue
cost could country course cover create cut dance dark date daughter day decide
decision deep degree desk develop did die different difficult dinner do doctor
dog door down draw dream drink drive drop during each ear early earth east easy
eat education effect egg eight either else email end enough enjoy enter even
evening ever every everybody everyone everything example expect expensive
experience explain eye face fact fall family far fast father feel feeling
feet few field fight figure fill film final finally find fine finger finish
fire first fish five floor flower fly follow following food foot football for
force forget form four free friend from front full fun future game garden gas
get girl give glad glass go goal god going good got great green ground group
grow guess guide guy hair half hand happen happy hard have head hear heart heat
help here high history hit hold home hope horse hospital hot hotel hour house
how however human hundred husband idea if imagine important in include
information inside interest interested interesting international interview
into introduce island issue it job join just keep key kid kill kind kitchen
know language large last late later laugh law learn leave left leg less let
letter level life light like line list listen little live local long look lose
love low lunch machine main make man many map market marry math may maybe
meaning meet member men message middle might mile milk million mind minute
miss mistake model mom moment money month morning most mother move movie much
music must my name national natural near need never new news next nice night
nine no nobody noise north note nothing notice now number of offer office
often oil old on once one only open opinion or order other our out outside
over own page pain paint paper parent park part party pass past pay peace
people perhaps person phone photo picture piece place plan plane plant play
please point police poor popular possible post power practice prefer prepare
present president pretty price print problem process produce product program
project promise protect public pull push put quality question quick quickly
quiet quite radio rain raise reach read ready real really reason receive
recent red remember repeat reply report research rest restaurant result return
rice rich ride right ring river road rock role room round rule run sad safe
safety said sale same save say school science sea search season seat second
see seem sell send serious service set seven several shall share she short
show shut sick side simple since sing sister sit six size skill sleep small
smile snow so social soft some someone something sometimes son song soon
sorry sound south speak special spend sport spring square stand star start
state stay step still stop store story street strong student study subject
success such suddenly summer sun support sure surprise swim table take talk
tall tax tea teach teacher team tell ten test than thank that their them
then there these they thing think third this those though thought thousand
three through time today together tomorrow tonight too took top total touch
town train travel tree trip trouble true try turn tv two type under
understand until up us use usual usually vacation value very video view
visit voice wait walk wall want war warm wash watch water way we wear
weather week weekend welcome well went were west what when where whether
which while white who whole why wife will win wind window winter wish with
within without woman women wonder word work worker world worry would write
year yes yesterday yet you young your yourself internet online website
password software blog download better best worst written
more site contact been click health used data should system policy available info
review privacy user general university mail management united item center made
development design address community area sign file link technology version
section found related security network access current control personal shop
board location text rate government shopping account digital previous image
department title description insurance property content private customer
article provide source author press teen stock training credit advanced select
register library action industry medical server application staff topic comment
financial working standard mobile payment equipment legal memory performance
single club additional latest gift court given event release request professional
major space committee similar reference baby energy delivery term central
original self council discussion entertainment agreement format least society
trade edition further association able already specific gold collection display
limited director beach upon period official land average done technical region
record direct conference environment district calendar style statement update
resource document material adult cheap finance mark individual plus edit percent
function unit global economic player submit amount risk thanks various production
commercial weight advertising treatment knowledge magazine error currently
construction protection loan wide beauty manager position taken sort known
engineering none lake annual corporate church method purchase active response
holiday along death speed discount yellow political increase base environmental
stuff storage entry nature availability summary mean growth agency king activity
copy although income cash employment overall package seen engine port album
regional administration institute double screen exchange electronic across
apply held printer effective organization selection lost tour menu volume cross
silver corporation solution mature addition supply lower necessary union advice
career military rental huge cable division object appropriate length actually
score client capital sample sent shown culture band lead choice registration
consumer airport foreign artist furniture channel mode structure fund allow
contract button male matter custom multiple distribution editor industrial
cause potential focus female responsible primary cancer tool foundation friendly
schedule communication purpose feature independent approach physical hill medicine
deal unique survey prior telephone animal population regular secure therefore
simply evidence station favorite option master valley recently probably built
blood worldwide improve connection hall larger impact transfer introduction
ship disease excellent paid perfect opportunity classic command express award
distance ensure extra especially budget operation warning wine vote forward
significant owner useful directly housing authority told traffic strategy agent
valid modern senior grand trial charge instead cool normal wrote entire
educational leading metal positive fitness greater likely alternative guest trust
universal solid presentation became orange prevent theme campaign improvement
guitar spirit challenge acceptance hire election suggest branch serve magic
smart gave avoid manage corner rank element birth virus interactive separate
quarter procedure leadership define religious column faith chain developer
identify avenue missing approximately domestic comparison mental sequence inch
attack damage reserve plastic truth counter failure dollar camp automatically
bridge movement baseball approval draft chart equal adventure profit assistant
advertisement parking workshop gone extension golden completely funny portable
electrical applicable pattern boat theatre earlier sponsor classical warranty
direction basketball assembly nuclear mouse signal criminal brain sexual
powerful false cast felt personnel soul promote flag advantage hello maintain
tourism priority savings brief iron straight queen clearly handle sweet
associate truck behavior enlarge frequently revenue measure duty bear gain
festival ocean lack depth whatever laptop exactly explore concept nearly
eligible reality forgot origin knew billion destination faster intelligence
bought route broken blow battle speech wire rural replacement tape strategic
judge economics apartment height zero speaker obtain recreation designer remain
marriage roll secret bath negative theater perform healthy translation injury
lawyer married proposal birthday fail toward slightly assist conduct legislation
jazz ultimate representative frequency minor physics rare spent extreme forecast
cycle contain rise scene hunter lady crystal famous writer chairman violence
academy gender permanent agriculture practical philosophy regulation reduction
nutrition recording junior secondary wonderful mine ticket prevention whom
soccer presence instant automatic healthcare majority ahead moon participation
scheme utility manner combination despite strength turkey proper fear principal
comfort kept appeal cruise bonus previously beat household achieve dress
dealer contemporary nearby exposure hide gambling refer luxury certainly indeed
newspaper slow removal easier mostly spot factory interior promotion relative
amazing clock identity hidden reasonable relief revision influence importance
planet recipe permit proof tennis prescription bedroom empty instance hole
specifically represent pair ideal stress cream discover fourth advisor evil
aware shape remains greatest
southern monthly expert track sixth puppy bite departure
proud excess disaster giant alarm ongoing dangerous garage exciting
unfortunately respectively pleasure honor eagle pants nurse prayer luck
cheese comic carefully appearance smoke craft cake apart fellow blind lounge
gross strongly cafe horror familiar capable till admission shoe victory sand
mainly actor seal fifth citizen prize absolute anytime dirty deck donate
alive temple prove thin exhibition cabinet sick tropical collect definitely
purple existence mutual prison everyday apparently exhibit throw trend visible
desert obviously handbook summit escape somewhat glance championship
impossible obvious explanation electricity arrival okay pottery emphasis aspect
workplace awesome crash lift closer shadow infection expense clinic healing
princess mall spray dad extend motorcycle yard tourist murder senator pour
dust hence entirely rescue occupation closely diary seriously elsewhere
pollution abroad roof demonstrate atmosphere kiss beast experiment overseas
pizza rush absence cluster whereas yoga lamp partial palace acceptable verify
globe copper ordinary ghost boss pride champion cloudy personally plenty
sentence throat ignore uniform somewhere vacuum recognize brass plaza survival
publish whenever lifetime pioneer venue athletic vital fairly coastal charity
intelligent obligation wake stupid harbor traveler realize regardless enemy
puzzle lucky latter thick repeat drum flood outcome appreciate casual lovely
smile indoor regularly pine tend gulf divorce shopper partly tradition candy
tiger hunt complaint boost scholarship mill chronic moral den finger pound
locate burn ourselves tobacco wooden tough incident conversation decrease
chest pension worship capability herself precision solve shorts minority
diverse ingredients sole sight clay weak refund reception wise correctly
geography integrity worry danger vitamin widely phrase genuine paradise
hybrid intermediate emotional leaf glory diesel versus combine overnight
geographic exceed rod fault introduce silk romantic generator examine sad
correction wolf slowly communicate rugby infant fluid kick meal hurt unlike
equation probability pot slip grass comply florist cherry achievement funeral
earrings pee passenger convenient manga silent literary egg pill theft
childhood facial talent flexibility seeker wisdom shoot boundary mint spin
deadline robot witness equipped powder assess entrance declaration noble
gospel shore knight loose recipient illness southeast pending teenage soap
triple jam unusual destruction increasingly migration disorder routine
basically conventional axis habitat median occupational animated judicial
adjustment hero bachelor attitude carpet lenses difficulty punk collective
coalition enrollment pace wage collector atlas dawn observation torture coat
restoration convenience opposition container defendant confirmation supervisor
wizard liver liable brochure petition recall antenna belief bikini shoulder
decor diameter doll refine bidder singer literacy attraction invite suppose
involve moderate terror thirty opposite rapidly ban assurance clerk vast
outline jeans metropolitan odd wrap mood favor quiz attractive occasion
victim careful beam arrive orchestra sunset moreover minimal lottery aside
adjustable essay discipline dialogue alphabetical trace disposal shut
voluntary consult greatly mask midnight commonly photographer inform coal
intent zoo largely pleasant announce arrow engagement rough weird lion
inspired blade oxygen cookie canyon merely arrangement stretch furthermore
cooperative sleeve cleaner cricket beef stroke strap crowd surf transformation
personality rainbow hook decline cord cloud facilitate valve proceed knife
shelf bookstore adopt incredible donation outer crop twelve founder decade
dispute adverse everywhere excerpt steam
expectations violation sunglasses builder socket basis bigger meeting
prohibited toner yield voltage dairy belong celebrate relate retailer
voters occupied reunion scroll drag virtually blend calculation lightning
duck wider cave dirt arise tribe buzz tile confident stunning meanwhile
delicious hobby emotions courage charitable frost geology revolutionary
chorus hungry lesser programmer democrat mold denial sticker dare
ambassador carnival moss mattress hardwood shine generous expired washer deaf
helicopter suburban upset thorough occurrence handmade
troops insider corn nationwide tech exposed immigration halloween artificial
tail assignment stadium preference spider grocery motel observer bench foam
hopefully greeting highlight distribute desperate recruiting relax verse
refuse wool invasion allied disagree scratch scientist polo fist calm horizon
inappropriate belly embassy canal ought lightweight intention fate shake
aquatic deserve witch elephant eleven refugees makeup allergy cigarette
lobby citizenship cement detective mentor stranger freeze cliff continually
adolescent purse painful delight shark likewise bless knock
pupils genetic independence undergraduate constantly depression guilty
interpretation parliament extract inquiry afraid wealth floral proven
hazardous honey substitute breath loud cure prompt wanna monkey identical
brunette trim daddy fold ugly fever rocket organize incorrect brick fool trap
flesh fare drain shame naughty barrel dude lamb marathon earthquake sandwich
memo accent workout blond scared magical wound pencil lazy
curve penalty underwear machinery liberty combat broker intro combo
bunch trainer maple ferry distinct chef sunshine afford fake adjust
corruption hospitality cube flip elderly athletes argue bare harmony
strict fabulous triangle boom circular stomach invitation grave kitty
legally lyric pork traveller herb eternal urgent basement punch
fascinating hometown tooth therapist skating triumph banana badly vanilla
hollow inexpensive distant employ
according daily isolated demonstration cloth appliance confused commander
passport mate veteran anxiety hardly joke recover nursery fraction happiness
hammer durable volleyball gorgeous taxi informal reflection fifty jail sink
succeed surely reef rhythm excuse astrology conspiracy trader privilege
interstate tsunami sudden sunrise silly algebra panic bacterial virtue
promptly refresh stripes lone destiny thriller proudly prisoner tragedy
harassment
announcement violent jury postage ecommerce charger sauce laundry
impressive socks dozen stylish bubble salad angry bean spotlight olive
silence excited authentic breed replica inspiration mechanics punishment
seafood anger headphones trio grip superb nerve patrol infinite aerial
poet revenge briefly kilometers helmet fleece tent wicked gasoline burst
motivated possess bunny dentists heater fatty idol afterwards dramatically
photograph reporter appointment hiking supplement radar illustrated quarterly
rack receiver concentration friendship harm tire spouse annually bull pitch
burden spare starter butter bride analyze rider recreational chess medal
transparent interact inspector shortly impression twist runner oval jungle
restriction vaccine curious tray hormone questionnaire rebel mild worthy
sofa settle mercy predict vegetation publicity invisible toddler productive
beginner colony aluminium elimination goat mixer mailman cradle poison
yearly crossword
vista shock creator fortune neutral substance involvement crown modification
arena understanding reading writing hang layout ease alien float decorative
tale oasis brush praise lover slim fancy parade nail homework invest artistic
officially mainland deeply oops shipment gather fork swap appreciation
trash bald smell badge peaceful garbage cite scholar rational violin picnic
obesity
deputy wedding fame cosmetic camel fundamental choose costume convert
planner struggle lecture learning reviewer terrorist northeast arcade counsel
stability gonna analyst mercury executive tune automobile judgment naturally
elite dimension soup establishment cassette baker gang trunk depend pixel
presidential compete unity suffer candle vessel proportion addiction shelter
treaty shield projector stamp salon fleet manufacture bend investigate critics
necklace wellness assault approve essentially participant oven toxic exotic
trigger tender retain stuck occasionally destroy nickname flame finite timber
lending collapse geometry medieval simultaneously donor reload precise moisture
struck cabin heavily envelope humanity bacteria defend slight geek protest
batch trivia horn breakdown arrange grab diploma devoted deny attract disco
prediction creativity lonely beneath trick bicycle hydrogen flavor passive
conclude handbags magnet dump acne softball civilian hint accurately journalist
wallet balloon textbook retreat lean tenant grateful floppy chubby abandoned
sewing perceived internationally tactics infectious researcher accommodate
allowance incomplete girlfriend qualification tide assign characteristic
continuity habits satisfactory efficiently savage magnificent sphere neighbor
unnecessary surgeon endangered ranger brutal opponent fridge sheriff translator
bloom cope telescope sail fireplace awful concentrate lately remind harmful
gentleman nightmare performer nest nationally manually
headset tablet recycling southwest fraud slave orientation requirement
weapon suspect sword brilliant pond viewer absent rarely buried snap
rotation hazard racial dive collar beverage alter encounter galaxy journalism
fence explosion impressed intersection organizer enormous dice myth granny
needle turtle temporarily missile disclose bother
receipt tissue graph means perfectly translate sheep autumn poem differ
architect cheat vegetable survive aggressive spell publicly roller reject
mess tattoo symphony marble fountain prepaid pirates microphone tomato
educated disappointed
passion assure duplicate simplified homeland
auto wireless username blogger reputation honest somebody mathematical
retired perfume bracelet passage
headquarters customs restore hood juice chase farmer killer instantly
railway protective eliminate mineral exploration
recorder portrait acrobat continental canvas cemetery nylon indicator
astronomy horizontal instructor simulation quit navigate shaved alternate
dose compression projection structural anybody initially seasonal scenario
satisfy extraordinary terrible pickup cheaper correspondence intensity
chaos microwave blast cargo bronze coin graduation chapel transform seventh
prep booth playlist snake minimize cooler motivation individually karaoke
nasty forty blame sprint cheers raid integrate ribbon peninsula pest penguin
shortcuts dock scout swift lighter adapted accomplish pastor fairy tackle
scenic threatening airplane knit glow gossip blanket baking wagon convicted
sexually deadly forbidden climb greenhouse dense pursuit
documentary defence popularity toilet bingo roommate nowhere mighty
periodic digit ozone conscious consequently unexpected immigrants
charm lawn cottage nose rocky ceiling grill grain mart soldier bomb deer
skirt noon
frequent tongue lung elegant interval reset suggestion celebration engage
pepper luggage salmon
sunny rabbit lemon darkness frozen ceremony precious brake exterior
unemployment profession electron royalty rally ripe advertiser
timely exclude exceptional qualify diagram footwear attach railroad organ
mixture
admit locally pole calcium harvest preserve
tunnel wheat cattle treasure knee injured immune reveal
dramatic execute pray offense invention supporters hourly butterfly
uncle steady complicated detect actively cookbook accompanied
frog innocent decent warrior
grammar
nervous berry bullet gods
invoice safely timer chick
soonest thumb gravity sculpture
penny imagination rope cowboy actress headline
airfare cashiers minus shade spice fighter genius barely literally politicians
wherever gentle freely meaningful partially
congratulations boring thunder potato onion pillow steal vocabulary junk
merry divide roughly physically
surround fifteen homeless horrible entertaining parcel cage renew alike caution
postcard vegetarian musician vampire garlic pasta miracle whale inbox
refrigerator peas sticky scary annoying dryer survivor
dumb endless waterproof pale jewel
bacon circus
hunger colleague aquarium librarian brave mysterious grams flour
bored
copyright shipping forum code index county media directory profile estate
thread category gallery series cart feedback login quote poker status browse
range seller audio analysis journal cell archive marketing database federal
centre subscribe newsletter golf domain chapter license hardware chat loss
brand advertise kingdom drug western commission casino mortgage rather
certain jewelry clothing particular bush logo statistics investment flash
browser navigation publisher overview accommodation assessment maximum retail
catalog programme input enterprise abstract output responsibility resolution
publication session photography republic century academic assistance skin grade
mountain filter vehicle longer northern panel match default require outdoor
otherwise protein transportation pool politics partner disclaimer faculty
membership mission sense pack stage internal goods unless race background
target except character maintenance ability battery youth pressure debt medium
television core throughout wood itself studio reader virtual device rent remote
external apple theory remove surface minimum visual host variety manual block
repair fair civil steel wrong beginning sector capacity jersey fully electric
officer driver dead respect unknown worth relationship farm traditional campus
creative coast benefit progress funding lord grant agree fiction museum
themselves transport evaluation former implementation zone complex jack flat
flow literature respective scale economy highest helpful critical frame musical
definition secretary path employee chief bottom detail royal switch largest
relevant guidelines justice connect basket weekly installation demand suite
attention advance skip diet army auction gear difference correct nation sheet
firm older species jump resort facility random certificate minister motion
fashion documentation monitor forest whose coverage couple chance vision
discuss accept automotive successful clinical situation lowest highly appear
emergency currency leather determine temperature palm patient actual historical
stone commerce scientific satellite village amateur particularly buyer
cultural easily oral poster edge functional root pink balance graduate shot
architecture initial label recommend league waste provider optional dictionary
accounting manufacturing chair fishing effort phase fantasy motor professor
context install shirt apparel generally mass crime count breast religion
claim permission surgery patch wild generation chemical task reduce himself
component enable exercise guarantee leader diamond alone keyword flight congress
fuel paperback pocket rose freedom argument competition joint premium fresh
attorney upgrade factor stream pick hearing eastern therapy upper administrative
prime limit creek quantity urban essential myself platform load affiliate labor
immediately nursing defense designated heavy recovery merchant comprehensive
compliance marine mount certified abuse native wholesale fort lighting senate
gene disc laser icon dedicated delete graphic atom anonymous script dining
alert integration framework criteria vice laboratory vintage checkout frank
zoom residential anime clip partnership editorial expression equity acid
cent compatible ministry portal beta lingerie postal administrator accuracy
dual pharmacy creation static dynamic portfolio exclusive vendor originally
toll cape import preview matrix amendment delta convention alpha certification
bookmark subscription signature provision layer liability trademark revised
optical conversion serial ratio onto bass bureau conservation yeah marketplace
evolution euro operator generic encyclopedia usage census peak competitive
exist wheel transit salt compact poetry angel bell preparation attempt
accordance width array accurate climate alcohol instruction annotation smaller
newest establish extent sharp lane paragraph mathematics compensation export
aircraft conflict employer occur percentage describe concern backup heritage
immediate spread coach agricultural expand audience participate plug specialist
cook affect experienced investigation institution totally plate indicate
blonde proceedings transmission organic seek extremely equivalent chemistry
neighborhood agenda anyway advisory curriculum logic template prince circle
soil anywhere psychology circumstances investor identification wildlife
elementary unlimited respond plain exit launch wave holy guidance mesh trail
enforcement symbol highway buddy hardcover dean setup poll glossary fiscal
celebrity bond appendix notify chocolate portion scope supplier cotton
require biology dental border ancient debate pregnancy biography leisure
notebook explorer historic disabled authorized crazy upcoming concert retirement
efficiency comedy efficient linear commitment specialty carrier constant visa
mouth meter reflect pure deliver fruit reform lens discovery classified
assume confidence alliance confirm neither engineer lifestyle consistent
replace clearance inventory organisation babe safari objective sugar crew stick
relation genre slide volunteer rear democratic enhance exact bound parameter
adapter processor formal contribute lock hockey storm micro bowl supreme
recognition tank submission estimate encourage navy regulatory inspection
cancel territory transaction delay pilot outlet continuous initiative novel
execution disability ultra winner contractor episode examination dish bulletin
modify truly painting extensive affordable universe candidate patent slot
outstanding perspective lodge messenger mirror tournament consideration
sterling gray catalogue broad demo hate terminal behalf liquid loop salary
reservation gourmet guard properly empire resume twenty newly illegal expansion
vary premier consent drama downtown keyboard contest boot suitable absolutely
audit chamber muscle implement typical tower calculator significantly chicken
temporary attend shower dear sufficient shell province awareness governor
beer contribution measurement formula constitution solar reliable consultation
northwest doubt earn finder unable classroom democracy wallpaper merchandise
resistance symptoms biggest memorial visitor twin forth insert gateway alumni
biological transition romance instrument split heaven pregnant twice
classification physician bargain cellular normally spiritual diabetes suit
shift chip flexible numerous relatively satisfaction superior cartoon
intellectual carbon comfortable magnetic interaction effectively registry
crisis outlook massive bright treat header poverty piano echo grid experimental
revolution consolidation plasma earnings mystery landscape dependent mechanical
journey banner applicant charter cooperation acquisition notification rapid
hairy diversity reverse deposit seminar specify accessibility sensitive
router concrete folder completion upload pulse technique calculate broadcast
metro anniversary strip specification pearl accident accessible accessory
resident plot possibly airline typically representation regard pump smooth
strike consumption narrow afternoon threat consultant controller ownership
legislative trailer castle antique willing molecular exam residence density
strange sustainable statistical mention innovation grey parallel operate bold
bathroom stable opera lesson cinema asset scan reaction blank entitled severe
generate stainless deluxe humor exception duration bulk successfully fabric
primarily tight contrast recommendation recruitment cute adoption capture
desire expertise mechanism jewellery welfare peer eventually innovative
massage rubber conclusion meat legend grace monster bang villa bone
collaboration detection inner formation tutorial entity gate holder moderator
settlement valuable tone ethics forever dragon captain fantastic neck wing
stereo appointed taste commit tiny operational rail liberal tube corresponding
belt jacket determination animation
`.trim().split(/\s+/))

const ordinaryAbbreviations = new Set(`
auto tech info pro fax lab pub stats beta alpha math demo app intro eco
web net map art law gay fun gas bus spa gym era jet pen joy dad ear tie toe
ion ban odd cry hub die bug tag ray kit hit pay sun run lot log car van pop
`.trim().split(/\s+/))

function regularBase(word: string, vocabulary: Set<string>): string | undefined {
  if (independentForms.has(word)) return undefined
  const candidates: string[] = []
  if (word.endsWith('ies')) candidates.push(`${word.slice(0, -3)}y`)
  if (word.endsWith('ied')) candidates.push(`${word.slice(0, -3)}y`)
  if (word.endsWith('s') && !word.endsWith('ss')) candidates.push(word.slice(0, -1))
  if (/(?:ches|shes|xes|zes|sses|oes)$/.test(word)) candidates.push(word.slice(0, -2))
  if (word.endsWith('zzes')) candidates.push(word.slice(0, -3))
  for (const suffix of ['ed', 'ing']) {
    if (!word.endsWith(suffix)) continue
    const root = word.slice(0, -suffix.length)
    candidates.push(root, `${root}e`)
    if (/(.)\1$/.test(root)) candidates.push(root.slice(0, -1))
  }
  return candidates.find(base => base.length > 1 && vocabulary.has(base))
}

function exclusionReason(word: string, vocabulary: Set<string>): string | undefined {
  if (word.length === 1 && word !== 'a' && word !== 'i') return '한 글자 단어'
  if (!/^[a-z]+$/.test(word)) return '숫자·기호가 포함된 항목'
  if ((word.length <= 3 && !shortWords.has(word)) ||
      (abbreviations.has(word) && !ordinaryAbbreviations.has(word)) ||
      formatTerms.has(word)) return '표기·약어'
  if (properNames.has(word)) return '고유명사(지명·인명·회사명 등)'
  if (spamTerms.has(word)) return '성인·스팸성 웹 검색어'
  const irregularBase = irregularRedundancies.get(word)
  if (irregularBase && (vocabulary.has(irregularBase) || ['gloves', 'bleeding', 'limousines', 'bestsellers', 'stockings', 'columnists', 'housewives'].includes(word))) {
    return `단순 변화형: ${irregularBase}`
  }
  const base = regularBase(word, vocabulary)
  if (base) return `단순 변화형: ${base}`
  if (easyWords.has(word)) return '쉬운 단어(토익 800+)'
  return undefined
}

async function collectEnglish() {
  const response = await fetchSource(englishUrl)
  if (!response.ok) throw new Error(`영어 목록 조회 실패: ${response.status}`)
  const raw = (await response.text()).split('\n').map(word => word.trim().toLowerCase()).filter(Boolean)
  const vocabulary = new Set(raw)
  const previous = await load('en')
  // 구 사전 응답 200개는 재사용하지 않는다. 직접 작성 방식의 배치만 이어 쓴다.
  const authored = new Map(previous?.metadata.selectionVersion === 2 || previous?.metadata.selectionVersion === 3 ? previous.words.map(word => [word.word, word]) : [])
  const source = emptySource([englishUrl], [raw.length])
  source.metadata.authoring = 'GPT가 IPA·품사·영어 뜻·한국어 뜻·예문·번역을 직접 작성'
  source.metadata.selectionVersion = 3
  source.metadata.excluded = []
  const seen = new Set<string>()
  for (const word of raw) {
    const reason = seen.has(word) ? '정규화 중복' : exclusionReason(word, vocabulary)
    seen.add(word)
    if (reason) {
      source.metadata.excluded.push({ word, reason })
      continue
    }
    const existing = authored.get(word)
    source.words.push(existing ? { ...existing } : {
      language: 'en', word, phonetic: null, meaningKo: '', meaningEn: null,
      example: null, exampleKo: null, level: 1, category: '', partOfSpeech: null, collocations: [],
    })
  }
  source.words.forEach((entry, index) => { entry.level = Math.floor(index * 5 / source.words.length) + 1 })
  await save('en', source)
  console.log(`영어 선별: ${source.words.length}개, 제외 ${source.metadata.excluded.length}개, 직접 작성 ${source.words.filter(word => word.meaningKo).length}개`)
}

async function main() {
  await collectChinese()
  await collectEnglish()
}

main().catch(error => {
  console.error('단어 수집 중단:', error instanceof Error ? error.message : String(error))
  process.exitCode = 1
})
