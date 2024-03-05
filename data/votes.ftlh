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
            <#list candidates?sort_by("amountOfVotes")?reverse as candidate>
                <div class="card">
                    <form action="/vote" method="post" class="flex flex-col align-center" href="#">
                        <img src="/images/${candidate.photo}">
                        <p>${candidate.name} -
                            <#if totalVotes != 0>
                                ${((candidate.amountOfVotes / totalVotes) * 100)?string("0.##")} % of all votes
                            <#else>
                                0 % of all votes
                            </#if>
                        </p>
                    </form>
                </div>
            </#list>
        </main>

        <p>-------------------------------------------------------</p>
        <a class="back flex align-center" href="/">Back to Main</a>
    </div>
</body>

</html>
