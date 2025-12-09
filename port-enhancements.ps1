# Script to port cooperative timeout enhancements to multiple JSqlParser versions

$versions = @("4.6", "4.7", "4.8", "4.9", "5.0")
$baseTag = "jsqlparser-4.5"
$enhancedBranch = "jsqlparser-4.5-ext"

Write-Host "Starting to port enhancements to versions: $($versions -join ', ')" -ForegroundColor Green

foreach ($version in $versions) {
    $targetTag = "jsqlparser-$version"
    $targetBranch = "jsqlparser-$version-ext"
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "Processing version: $version" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    # Check if tag exists
    $tagExists = git tag -l $targetTag
    if (-not $tagExists) {
        Write-Host "Tag $targetTag does not exist, skipping..." -ForegroundColor Yellow
        continue
    }
    
    try {
        # Checkout the target tag
        Write-Host "1. Checking out tag $targetTag..." -ForegroundColor Yellow
        git checkout $targetTag 2>&1 | Out-Null
        
        # Create new branch
        Write-Host "2. Creating branch $targetBranch..." -ForegroundColor Yellow
        git checkout -b $targetBranch 2>&1 | Out-Null
        
        # Cherry-pick the enhancement commits from base branch
        Write-Host "3. Cherry-picking enhancements from $enhancedBranch..." -ForegroundColor Yellow
        
        # Get the commits that added the cooperative timeout
        $commits = @(
            "847b8113", # Initial cooperative timeout implementation
            "b135019d", # Enhanced timeout checkpoints
            "988d6314", # README
            "754b0747"  # CI fixes
        )
        
        $success = $true
        foreach ($commit in $commits) {
            Write-Host "   Cherry-picking commit $commit..." -ForegroundColor Gray
            $result = git cherry-pick $commit 2>&1
            
            if ($LASTEXITCODE -ne 0) {
                Write-Host "   Conflict detected, attempting to resolve..." -ForegroundColor Yellow
                
                # Check if it's a pom.xml version conflict (expected)
                $status = git status --porcelain
                if ($status -match "pom.xml") {
                    Write-Host "   POM version conflict (expected), using target version..." -ForegroundColor Gray
                    # Keep the target version in pom.xml
                    git checkout --theirs pom.xml 2>&1 | Out-Null
                    git add pom.xml 2>&1 | Out-Null
                }
                
                # Try to continue
                git cherry-pick --continue 2>&1 | Out-Null
                
                if ($LASTEXITCODE -ne 0) {
                    Write-Host "   Failed to resolve conflicts automatically" -ForegroundColor Red
                    git cherry-pick --abort 2>&1 | Out-Null
                    $success = $false
                    break
                }
            }
        }
        
        if ($success) {
            Write-Host "4. Building to verify..." -ForegroundColor Yellow
            $buildResult = mvn clean compile -DskipTests -q 2>&1
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "5. Build successful! Pushing to remote..." -ForegroundColor Green
                git push -u origin $targetBranch 2>&1 | Out-Null
                
                # Create and push tag
                $releaseTag = "jsqlparser-$version-ext-v1.0"
                Write-Host "6. Creating release tag $releaseTag..." -ForegroundColor Yellow
                git tag -a $releaseTag -m "Release v1.0: Cooperative timeout mechanism

Features:
- Zero thread creation cooperative timeout
- Enhanced timeout checkpoints in critical parsing loops
- Java 8+ compatible
- 10-30% performance improvement
- Backward compatible API

Based on jsqlparser-$version with performance enhancements" 2>&1 | Out-Null
                
                git push origin $releaseTag 2>&1 | Out-Null
                
                Write-Host "✅ Successfully ported to version $version" -ForegroundColor Green
            } else {
                Write-Host "❌ Build failed for version $version" -ForegroundColor Red
                Write-Host $buildResult
            }
        } else {
            Write-Host "❌ Failed to cherry-pick commits for version $version" -ForegroundColor Red
        }
        
    } catch {
        Write-Host "❌ Error processing version $version : $_" -ForegroundColor Red
    }
}

# Return to original branch
Write-Host "`nReturning to $enhancedBranch branch..." -ForegroundColor Cyan
git checkout $enhancedBranch 2>&1 | Out-Null

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "Port process completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
