<!-- candidates_page.ftl -->

<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <title>Vote for us!</title>
    <link rel="stylesheet" href="css/candidates.css">
</head>

<body>
    <div class="container flex flex-col align-center">
        <h1>Vote for us</h1>
        <main class="flex flex-wrap align-evenly">
            <#list candidates as candidate>
                <div class="card">
                    <form action="/" method="post" class="flex flex-col align-center" href="#">
                        <img src="images/${candidate.photo}">
                        <p>${candidate.name}</p>
                        <input name="candidateIndex" type="hidden" value="${candidate?index}">
                        <button type="submit" class="vote flex align-center">Vote</button>
                    </form>
                </div>
            </#list>
        </main>

        <p>-------------------------------------------------------</p>
        <a class="back flex align-center" href="/votes">Show total votes</a>
    </div>
</body>

</html>
