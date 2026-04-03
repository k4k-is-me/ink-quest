if exist .\output.txt del .\output.txt

for /r %%f in (*.java) do (
    echo --- %%f --- >> .\output.txt
    type "%%f" >> .\output.txt
    echo. >> .\output.txt
)

echo Готово! Файл output.txt создан
pause