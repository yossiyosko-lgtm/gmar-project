# Makefile - קובץ קיצורי דרך לפקודות נפוצות בפרויקט.
# מריצים עם: make <שם-טארגט>, למשל make build
# .PHONY אומר ל-make שאלה לא שמות של קבצים אמיתיים - שיריץ אותם תמיד ולא ידלג אם "קיים קובץ" בשם הזה.
.PHONY: build test run package clean health docker-up docker-down

# build - מקמפל את קוד ה-Java בלי ליצור jar, הכי מהיר לבדוק שאין שגיאות תחביר.
build:
	mvn compile

# test - מריץ את כל הטסטים (JUnit). זו הפקודה שגם ה-CI מריץ בהמשך.
test:
	mvn test

# package - בונה קובץ jar אחד שניתן להריץ, כולל כל התלויות (Gson, AWS SDK וכו').
package:
	mvn package

# run - מריץ את קובץ ה-jar שנבנה. תלוי בכך שכבר הרצת "make package" לפני כן.
run:
	java -jar target/care-circle.jar

# health - פקודת עזר: שולח בקשת curl ל-/health כדי לבדוק שהשרת חי ורץ.
health:
	curl -s localhost:8080/health

# clean - מוחק את כל קבצי הבנייה (תיקיית target/) כדי להתחיל בנייה נקייה מאפס.
clean:
	mvn clean

# docker-up - מרים את כל המערכת ברקע דרך docker compose (שלב 3 ואילך).
docker-up:
	docker compose -f docker/docker-compose.yml up -d --build

# docker-down - מכבה ומנקה קונטיינרים (שלב 3 ואילך).
docker-down:
	docker compose -f docker/docker-compose.yml down