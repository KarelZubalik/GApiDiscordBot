Pro správnou funkci je potřeba:
Mít na https://discord.com/developers/docs/intro vytvořeného bota + jeho token zkopírovat do souboru co nazveme tokenFolder.txt
Mít na https://developers.google.com/photos vytvořenou api + credentials na naši api. Tyto údaje následně stáhneme a soubor přejmenujeme na googleApi.json

Z kódu vytvořit jar snapshot přes maven commands package.
Vytvořit samostatnou složku s názvem např. DiscordBot. 
Do této složky přidáme soubor s discord tokenem a následně i json soubor na google api. 
Poté je potřeba vytvořit podsložku s názvem "jar" do té vložíme náš snapshot, který si můžeme přejmenovat třeba také na DiscordBot

Přes java -jar command můžeme spustit tento jar.
OAUTH2:
Pokud budeme spouštět na windows, tak se nám otevře prohlížeč a bude chtit autorizovat náš google účet, který máte uložený v prohlížeči. 
Autorizujte a program si sám přebere token a naběhne.

Pokud spouštíte na linuxu, tak vám program vygeneruje url a tu následně spustíte na vašem prohlížeci.
Autorizujete a vyskočí vám stránka, kde z URL vytáhnete kód, který následně přidáte do konzole běžícího programu.

Poté se program spustí a vše naběhne. 

Následně už jen nastavíte práva botovi na serveru, kde je bot přidán. 

Commands, které zadáváte v discord channelu:

"send" Vyvolá jednu random fotku z google api galerie.
"send 1-5" Vyvolá 1-5 random fotku/y z google api galerie.