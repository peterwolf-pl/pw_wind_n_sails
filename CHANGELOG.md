# Changelog

## 1.0.1

### 🚩 Maszt flagowy i bandera (Flagpole & Animated Wind Flag)
- **7-blokowy maszt flagowy (`pw_wind_n_sails:flagpole`)**: Smukła konstrukcja z ciemnoszarego metalu, zwieńczona obrotową głowicą.
- **Płynna animacja tkaniny (`FlagModel`)**: 3-segmentowa bandera reaguje w czasie rzeczywistym na kierunek i siłę wiatru.
- **16 wariantów kolorystycznych**: Każdy postawiony maszt otrzymuje losowo jeden z 16 kolorów barwników vanilla.
- **Flauta i słaby wiatr**: Przy braku wiatru bandera realistycznie opada wzdłuż masztu (`limp`), a jej falowanie uspokaja się.
- **Desynchronizacja animacji wielu flag**: Każdy maszt posiada unikalny hash przestrzenny na podstawie współrzędnych bloku (indywidualne przesunięcie fazy, wariancja prędkości falowania $\pm 12\%$, mikroszkwał i amplituda), dzięki czemu stojące obok siebie flagi nie machają synchronicznie, lecz falują organicznie i niezależnie.
- **Receptura rzemieślnicza**: Tworzona ze sztabek żelaza, płotka dębowego i dowolnej wełny.

### ⛵ Zwijanie żagli, ster i szczelny kokpit
- **Zwijanie i rozwijanie żagla**: Klawisz **X** lub kliknięcie PPM na maszt pozwala zwinąć żagiel w porcie lub podczas manewrów.
- **Autocentryzacja steru**: Po zwolnieniu klawiszy A/D płetwa sterowa i rumpel natychmiast wracają do pozycji neutralnej.
- **Suchy kokpit (`water_patch`)**: Zastosowano natywną maskę wodną Minecrafta, całkowicie eliminując przenikanie wody przez dno łodzi.
- **Ochrona przed utknięciem**: Kadłub jest automatycznie unoszony przy wpływie na mielizny (`liftOutOfShore`), chroniąc przed zakleszczeniem w dnie.

### ⚖️ Balansowanie łódki (Balastowanie / Hiking) i poprawny kierunek przechyłu
- **Poprawiony kierunek przechyłu**: Żaglówka pod wpływem bocznego naporu wiatru przechyla się w poprawną stronę – na stronę zawietrzną (leeward, w kierunku, w który wieje wiatr).
- **Balastowanie na burcie nawietrznej (Spacja / Spacja x2 szybko)**:
  - **Siedzenie na burcie (pojedyncza spacja)**: sternik przesiada się na burtę od strony nawietrznej (`windward gunwale`), w dużej mierze równoważąc przechył łódki — kadłub prostuje się i stabilizuje na fali.
  - **Stanie na burcie (spacja 2 razy szybko)**: przy bardzo silnym wietrze i gwałtownych szkwałach sternik staje wyprostowany na krawędzi burty (`standing on gunwale`), uzyskując maksymalne ramię momentu prostującego i utrzymując łódź całkowicie pionowo nawet w warunkach sztormowych.
  - **Animacja stojąca**: dedykowany mixin `AvatarRendererMixin` prostuje nogi postaci ze standardowej pozycji siedzącej, stawiając model gracza stopami bezpośrednio na drewnianej burcie.
  - **Powrót na środek**: ponowne naciśnięcie spacji przywraca gracza na środek ławki rufowej.
  - **Dynamiczny HUD**: wskaźnik telemetrii precyzyjnie informuje o trybie balastowania (`Środek`, `Siedzi na burcie`, `Stoi na burcie`) i podpowiada odpowiednią akcję.

### 💨 Zaawansowana losowość wiatru i dobowe profile pogody
- **Dobowe profile pogodowe (`DailyWindProfile`)**: Każdy dzień w świecie gry charakteryzuje się unikalnym stanem atmosferycznym generowanym deterministycznie z ziarna świata.
- **Dni całkowitej flauty (`CALM`)**: Zdarzają się dni bezwietrzne, w których siła wiatru spada do `0.01 – 0.04`, woda staje się taflą lustra, a żagle tracą ciąg.
- **Zmienne kierunki w różne dni**: Każdy dzień ma niezależny kierunek przeważający w pełnym zakresie $0^\circ – 360^\circ$ (N, NE, E, SE, S, SW, W, NW), z płynną rotacją wiatru na przełomie nocy i świtu.
- **Zróżnicowana szkwalistość**: Od dni sztormowych z częstymi, mocnymi porywami (`+30% – +70%`) po dni spokojne z rzadkimi powiewami mocniejszymi zaledwie o kilka procent (`+2% – +6%`).
- **Cykl dzień vs noc (termika i wietrzne noce)**: W ciągu dnia konwekcja słoneczna wzmacnia wiatr, w nocy wiatr z reguły cichnie, ale pojawiają się też wietrzne noce (`windyNight`).
- **Równiejszy wiatr nocą**: W nocy częstotliwość szkwałów spada o 75%, ich siła i odchyłki są tłumione, a przepływ staje się stabilny i laminarny.

### 🌊 Prędkość żaglówki zależna od siły wiatru
- **Zatrzymanie przy flaucie**: Przy wietrze poniżej `0.06` ciąg aerodynamiczny żagla wynosi 0, a łódka całkowicie wyhamowuje do zera bez pełzania.
- **Proporcjonalne osiągi**: Wprowadzono falowy opór kadłuba (`hullDrag`), dzięki czemu prędkość łodzi bezpośrednio zależy od siły wiatru:
  - Flauta (< 0.06): 0.0 kn
  - Słaby wiatr (0.20): ~4 – 6 kn
  - Umiarkowany wiatr (0.50): ~11 – 15 kn
  - Silny wiatr (0.85): ~20 – 26 kn
  - Sztorm / szkwały (1.10+): ~28 – 34 kn

### 🧭 Jednoliniowy HUD wiatru (Klawisz H) i prognoza
- **Jednoliniowy HUD wiatru (`WindHudOverlay`)**: Naciśnięcie klawisza **H** włącza dyskretny pasek u góry ekranu:
  - Bieżący kierunek i siła wiatru w węzłach.
  - Kierunek, prędkość i odchyłka szkwału (lub informacja o spokojnym wietrze).
  - Prognoza pogody na najbliższy czas (trendy zmian, nadchodząca flauta, skręty wiatru o świcie).
- **Integracja z PeterWolf's Planes**:
  - HUD LIFT wznoszeń paralotni wyświetla się **tylko** podczas aktywnego lotu paralotnią.
  - Gdy gracz nie leci samolotem ani paralotnią, klawisz H przełącza HUD wiatru bez konfliktów.
- **Nowe polecenia diagnostyczne**:
  - `/windsails wind` – szczegółowe dane telemetryczne wiatru i prognoza pogody.
  - `/windsails wind set <siła> [kierunek]` – ustawianie parametrów wiatru przez administratorów (w tym testowej flauty `0.0`).
  - `/windsails environment [true|false]` (alias: `/windsails env`) – przełączanie wpływu wiatru na otoczenie (dym, drzewa, liście).

### 🍃 Wpływ wiatru na dym, drzewa i liście (`/windsails env`)
- **Dym z ognisk i pieców (`CampfireSmokeParticle`, `SmokeParticle`, `LargeSmokeParticle`)**:
  - Płynne znoszenie dymu z wiatrem – cząsteczki dymu układają się w ciągłe, znoszone smugi dokładnie według wektora kierunku i siły wiatru.
  - Przy flaucie dym wznosi się pionowo ku górze, przy silnym wietrze i szkwałach kładzie się nisko i szybko przemieszcza.
- **Spadające liście (`FallingParticle`)**:
  - Spadające liście wiśni, bladego dębu i topoli są unoszone i znoszone horyzontalnie przez wiatr proporcjonalnie do jego prędkości.
- **Drzewa i korony leśne (`TreeWindAmbienceHandler`)**:
  - Wiatr wiejący przez korony drzew (`#minecraft:leaves`) zrywa liście odpowiadające dokładnemu kolorowi ulistnienia danego drzewa (`ParticleTypes.TINTED_LEAVES`), unosząc je w powietrzu ze świstem wiatru.
  - Częstotliwość zrywania liści zależy bezpośrednio od siły wiatru i gwałtowności szkwałów (od pojedynczych powiewów do gęstych strumieni liści w wichurze).
  - W umiarkowanym i silnym wietrze w koronach drzew odtwarzany jest subtelny dźwięk szumu i trzepotu liści.
- **Pełna kontrola komendą**:
  - Komenda `/windsails environment <true|false>` lub `/windsails env` pozwala włączyć lub wyłączyć wpływ wiatru na otoczenie w dowolnym momencie.

### 🌊 Wybór stylów wskaźnika wiatru na wodzie (Cykl klawiszem TAB)
- **Cykl 5 stylów wizualnych pod klawiszem TAB**:
  1. `1/5: Jasna piana morska (Biel)` — wyraźne, jaskrawe białe grzbiety i smugi wiatrowe (biała piana na falach), doskonale widoczne na każdej wodzie.
  2. `2/5: Jasny błękit (Cyjan)` — świetlisty błękit nieba, dynamiczny i czytelny z daleka.
  3. `3/5: Bursztynowy wiatr (Złoto)` — ciepłe złote smugi o wysokim kontraście względem ciemnej toni wody.
  4. `4/5: Subtelny błękit (Łagodny)` — pastelowy, łagodny błękit nie narzucający się w polu widzenia.
  5. `5/5: Ciemny cień wody (Grafit)` — grube pasy zaciemnienia tafli wody (kocie łapy / zmarszczki wiatrowe), idealnie imitujące cienie rzucane przez wiatr na wodę.
  6. `Wyłączone` — całkowite wyłączenie wskaźników.
- **Gruba, opływowa geometria**: Wszystkie style używają szerokich, aerodynamicznych pasów i grotów (`RenderTypes.debugQuads()`) o grubości `~0.32 – 0.50` bloku, leżących bezpośrednio na powierzchni wody.
- **Komunikat wyboru**: Każde naciśnięcie TAB wyświetla na ekranie nazwę i numer wybranego stylu.

---

## 1.0.0

- Pierwsze oficjalne wydanie Peterwolf's Wind & Sails na **Minecraft 26.3 (Fabric)**.
- Jednomasztowa łódź żaglowa z ruchomym bomem, żaglem i sterem z rumplem.
- Realistyczny model aerodynamiczny oparty na apparent wind, kątach natarcia, halsowaniu i przechyłach bocznych.
- Dynamiczne trymowanie szota klawiszami **W / S** i sterowanie sterem **A / D**.
- Wizualizator smug wiatru na wodzie pod klawiszem **TAB**.
- Zintegrowany HUD żeglarski z prędkościomierzem w węzłach i kątami natarcia.
